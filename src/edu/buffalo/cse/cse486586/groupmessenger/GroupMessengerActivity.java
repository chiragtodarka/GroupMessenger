package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;


import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class GroupMessengerActivity extends Activity 
{
	static String avdName = "";
	static int uniqueMesssageNumber = 0;
	static String chatHistory = "";
	static String tcpString = "10.0.2.2";

	static TextView textView;

	static int expectedSequenceNumber = 0;
	static int sequenceNumber = 0;

	static ConcurrentHashMap<String, Packet> holdBackQueue;
	static ConcurrentHashMap<String, Packet> sequencerHoldBackQueue;
	static ConcurrentHashMap<String, Packet> causalHoldBackQueueAVD0;
	static ConcurrentHashMap<String, Packet> causalHoldBackQueueAVD1;
	static ConcurrentHashMap<String, Packet> causalHoldBackQueueAVD2;

	private Uri mUri;

	int[] vector;
	int[] sequencerVector;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);

		textView = (TextView) findViewById(R.id.textView1);

		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());
		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));



		// Code taken from
		// https://docs.google.com/document/d/1JqBqZChFWzbnTXgbYB8Z6l9IRlzt8c0loIwa6ymkPps/pub
		TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		// end

		if (portStr.equals("5554")) 
		{
			avdName = "avd0";
		} 
		else if (portStr.equals("5556")) 
		{
			avdName = "avd1";
		} 
		else if (portStr.equals("5558")) 
		{
			avdName = "avd2";
		}

		holdBackQueue = new ConcurrentHashMap<String, Packet>();
		sequencerHoldBackQueue = new ConcurrentHashMap<String, Packet>();
		causalHoldBackQueueAVD0 =  new ConcurrentHashMap<String, Packet>();
		causalHoldBackQueueAVD1 =  new ConcurrentHashMap<String, Packet>();
		causalHoldBackQueueAVD2 =  new ConcurrentHashMap<String, Packet>();
		mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider");

		vector = new int[3];
		vector[0] = 0;
		vector[1] = 0;
		vector[2] = 0;
		sequencerVector = new int[3];
		sequencerVector[0] = 0;
		sequencerVector[1] = 0;
		sequencerVector[2] = 0;

		new MyServer().executeOnExecutor(SendMessage.THREAD_POOL_EXECUTOR, 10000);

	}// onCreate() ends

	private Uri buildUri(String scheme, String authority) 
	{
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}

	public void send(View view)
	{
		final EditText editText = (EditText) findViewById(R.id.editText1);
		String messageForServer = editText.getText().toString();
		messageForServer = messageForServer.replace("\n", "");
		messageForServer = messageForServer.replace("\r", "");
		messageForServer = messageForServer.trim();

		if(!messageForServer.trim().equals(""))
		{
			if(messageForServer.length()<129)
			{
				Log.v("messageForServer", messageForServer);
				//new SendMessage().execute(messageForServer, chatHistory);
				createAndSendPacket(messageForServer);
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Message Length cant exceed 128 characters", Toast.LENGTH_SHORT).show();
			}

			editText.setText("");
			editText.setHint("Enter message");

		}    
	}// send() ends

	public void createAndSendPacket(String message)
	{
		Packet packet = new Packet();
		packet.message = message;
		packet.avdName = avdName;
		packet.uniqueMesssageNumber = uniqueMesssageNumber;
		packet.fromSequencer = false;
		packet.sequenceNumber = -1;
		packet.istest2 = false;

		if(message.equals("test2"))
			packet.istest2 = true;

		int fromAvd = Integer.parseInt(packet.avdName.charAt(3)+"");

		packet.packetVector = new int[3];
		vector[fromAvd] = vector[fromAvd]+1;
		packet.packetVector[0] = vector[0];
		packet.packetVector[1] = vector[1];
		packet.packetVector[2] = vector[2];


		bMultiCast(packet);
	}

	public void bMultiCast(Packet packet)
	{
		new SendMessage().execute(packet);
	}

	private class MyServer extends AsyncTask<Integer, Packet, Void>
	{

		protected void onProgressUpdate(Packet... packet) 
		{
			super.onProgressUpdate(packet[0]);
			textView.append(packet[0].avdName+":"+packet[0].message+"\n");
		}

		@Override
		protected Void doInBackground(Integer... params) 
		{
			try
			{
				ServerSocket serverSocket = new ServerSocket(10000);

				while(true)
				{
					Socket serverClientSocket = serverSocket.accept();
					ObjectInputStream ois = new ObjectInputStream(serverClientSocket.getInputStream());
					Packet packet = (Packet)ois.readObject();
					Log.v("PacketReceived", "PacketReceived");
					Log.v("packet.FromAvd",packet.avdName);
					Log.v("packet.uniqueMesssageNumber", packet.uniqueMesssageNumber+"");
					Log.v("messgae", packet.message);
					Log.v("expectedSequenceNumber",expectedSequenceNumber+"");


					if(packet.fromSequencer)
					{
						Log.v("from sequencer", "from sequencer");
						Log.v("sequenceNumber in packet",packet.sequenceNumber+"");

						sequencerHoldBackQueue.put(packet.avdName+":"+packet.uniqueMesssageNumber, packet);

						while(true)
						{
							boolean contains = false;
							Iterator<String> sequencerHoldBackQueueIterator = sequencerHoldBackQueue.keySet().iterator();
							Packet tempPacket;
							String key = "";

							while(sequencerHoldBackQueueIterator.hasNext())
							{
								key = sequencerHoldBackQueueIterator.next();
								tempPacket = sequencerHoldBackQueue.get(key);

								if(tempPacket.sequenceNumber == expectedSequenceNumber)
								{
									Packet tempPacketMain = tempPacket;

									if(holdBackQueue.containsKey(key))
										tempPacketMain =	holdBackQueue.get(key);

									publishProgress(tempPacketMain);

									ContentValues contentValues = new ContentValues();
									String seqNum = tempPacketMain.sequenceNumber+"";
									contentValues.put("key", seqNum);
									contentValues.put("value", tempPacketMain.message);
									getContentResolver().insert(mUri, contentValues);

									expectedSequenceNumber++;

									sequencerHoldBackQueue.remove(key);

									if(holdBackQueue.containsKey(key))
										holdBackQueue.remove(key);

									if(tempPacketMain.istest2)
									{
										new Test2Thread().start();
									}


									contains = true;
								}// if block
								else
								{
									contains = false;
								}

							}//innner loop

							if(!contains)
								break;

						}//outer loop
						Log.v("incrementing", "expected seq number");
					}// if(packet.fromSequencer) ends
					else
					{
						Log.v("Not from sequencer", "Not from sequencer");
						Log.v("Adding data", "adding data");
						holdBackQueue.put(packet.avdName+":"+packet.uniqueMesssageNumber, packet);
						
						if(GroupMessengerActivity.avdName.equalsIgnoreCase("avd0"))
						{
							//Code for sequencer
							Log.v("sequencer executing", "sequencer executing");

							if(packet.avdName.equals("avd0"))
							{
								Packet newPacket = new Packet();
								newPacket.avdName = packet.avdName;
								newPacket.uniqueMesssageNumber = packet.uniqueMesssageNumber;
								newPacket.message = packet.message;
								newPacket.istest2 = packet.istest2;

								newPacket.fromSequencer = true;
								newPacket.sequenceNumber = sequenceNumber;

								bMultiCast(newPacket);
								sequenceNumber++;
								sequencerVector[0] = packet.packetVector[0];
							}

							if(packet.avdName.equals("avd1"))
							{
								causalHoldBackQueueAVD1.put(packet.avdName+":"+packet.uniqueMesssageNumber, packet);

								while(true)
								{
									boolean contains = false;

									Iterator<String> iterator = causalHoldBackQueueAVD1.keySet().iterator();
									String key = "";
									while(iterator.hasNext() && (causalHoldBackQueueAVD1.size()>0))
									{
										key = iterator.next();
										Packet pocket = causalHoldBackQueueAVD1.get(key);

										if(pocket.packetVector[1] == (sequencerVector[1]+1))
										{
											contains = true;
											Packet newPacket = new Packet();
											newPacket.avdName = pocket.avdName;
											newPacket.uniqueMesssageNumber = pocket.uniqueMesssageNumber;
											newPacket.message = pocket.message;
											newPacket.istest2 = pocket.istest2;

											newPacket.fromSequencer = true;
											newPacket.sequenceNumber = sequenceNumber;

											bMultiCast(newPacket);
											sequenceNumber++;
											sequencerVector[1] = pocket.packetVector[1];
											causalHoldBackQueueAVD1.remove(key);

										}
									}

									if(contains==false)
										break;

								}
							}

							if(packet.avdName.equals("avd2"))
							{
								causalHoldBackQueueAVD2.put(packet.avdName+":"+packet.uniqueMesssageNumber, packet);

								while(true)
								{
									boolean contains = false;

									Iterator<String> iterator = causalHoldBackQueueAVD2.keySet().iterator();
									String key = "";
									while(iterator.hasNext() && (causalHoldBackQueueAVD2.size()>0))
									{
										key = iterator.next();
										Packet pocket = causalHoldBackQueueAVD2.get(key);

										if(pocket.packetVector[2] == (sequencerVector[2]+1))
										{
											contains = true;
											Packet newPacket = new Packet();
											newPacket.avdName = pocket.avdName;
											newPacket.uniqueMesssageNumber = pocket.uniqueMesssageNumber;
											newPacket.message = pocket.message;
											newPacket.istest2 = pocket.istest2;

											newPacket.fromSequencer = true;
											newPacket.sequenceNumber = sequenceNumber;

											bMultiCast(newPacket);
											//new SendMessage().execute(newPacket);
											sequenceNumber++;
											sequencerVector[2] = pocket.packetVector[2];
											causalHoldBackQueueAVD2.remove(key);
										}
									}

									if(contains==false)
										break;

								}
								
							}//if(packet.avdName.equals("avd2")) ends
							
						}//Sequencer ends ==> if(GroupMessengerActivity.avdName.equalsIgnoreCase("avd0")) ends
						
					}// else of if(packet.fromSequencer) ends
					
				}// while(true) for making server run continueously
				
			}// try ends
			catch (Exception e) 
			{
				Log.v("Error1", e.toString());
			}

			return null;
		}//doInBackground() method ends

	}// MyServer class ends

	private class SendMessage extends AsyncTask<Packet, Void, Packet> 
	{
		protected Packet doInBackground(Packet... packetArray) 
		{
			int[] portArray = new int[3];
			portArray[0] = 11108;
			portArray[1] = 11112;
			portArray[2] = 11116;

			for(int i =0; i<portArray.length; i++)
			{
				Log.v("in for(port)", portArray[i]+"");
						try
						{
							Socket clientSocket = new Socket(tcpString, portArray[i]);
							Log.v("ConnectionEstablished", "ConnectionEstablished");
							ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
							objectOutputStream.writeObject(packetArray[0]);
							//objectOutputStream.close();
						}
						catch (Exception e) 
						{
							Log.v("Error2", e.toString());
						}    
			}

			if(!packetArray[0].fromSequencer)
			{
				uniqueMesssageNumber++;
				Log.v("uniqueMesssageNumber in client",uniqueMesssageNumber+"");
			}

			return packetArray[0];
		}// doInBackground() ends
	}//SendMessage class ends


	public void test1(View view)
	{
		Test1Thread test1Thread = new Test1Thread();
		test1Thread.start();
	}

	private class Test1Thread extends Thread
	{
		public void run()
		{
			int counter = 5;

			for(int i=0; i<counter; i++)
			{
				createAndSendPacket(avdName+":"+i);
				try 
				{
					sleep(3000);
				} 
				catch (InterruptedException e) 
				{
					Log.e("error in sleep", e.getMessage());
				}
			}
		}

	}//Test1Thread class ends

	public void test2(View view)
	{
		createAndSendPacket("test2");
	}

	private class Test2Thread extends Thread
	{
		public void run()
		{

			createAndSendPacket(avdName+":"+"0");
			try 
			{
				sleep(500);
			} 
			catch (InterruptedException e) 
			{
				Log.e("error", e.getMessage());
			}
			createAndSendPacket(avdName+":"+"1");

		}
	}


}// GroupMessengerActivity class ends

@SuppressWarnings("serial")
class Packet implements Serializable
{
	String message;
	String avdName;
	int uniqueMesssageNumber;
	boolean fromSequencer;
	int sequenceNumber;
	boolean istest2;
	int[] packetVector; 

	public Packet()
	{
		super();
	}

}
