package edu.buffalo.cse.cse486586.simpledynamo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDynamoProvider extends ContentProvider {

	static final String TAG = SimpleDynamoActivity.class.getSimpleName();

	String[] REMOTE_PORTS = {"5554", "5556", "5558", "5560", "5562"};
	static final int SERVER_PORT = 10000;
	String myPort;
		String portStr;
		List<String> nodesConnected = new ArrayList<String>();
		HashMap<String, String> encodedPort = new HashMap<String, String>();
		final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.simpledht");
		SQL_Helper chordSQLHelper;
		int size;

		private Uri buildUri(String scheme, String authority) {
			Uri.Builder uriBuilder = new Uri.Builder();
			uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}


	@Override
	public boolean onCreate() {
		// TODO Auto-generated method stub

		TelephonyManager tel = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
		portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		chordSQLHelper = new SQL_Helper(getContext());
		for (String port : REMOTE_PORTS) {
			try {
				encodedPort.put(genHash(port), port);
				nodesConnected.add(genHash(port));

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
        Collections.sort(nodesConnected);
		size = nodesConnected.size();
		Log.e("Create", "Nodes : " + nodesConnected);



		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
			Log.e("ServerCreated", "Server is created");
		} catch (IOException e) {
			Log.e(TAG, "Can't create a ServerSocket");
			e.printStackTrace();
		}
		SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
		Cursor locCur = messenger_db.rawQuery("select * from " + FeedReaderContract.FeedEntry.TABLE_NAME, null);
		int numRows = locCur.getCount();
		if (numRows>0)
			new ClientTaskRestore().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,"Restore");

//		MatrixCursor cur = new MatrixCursor(new String[]{"key","value"});
//		Integer ind = null;
//		try {
//			ind = nodesConnected.indexOf(genHash(portStr));
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
//
//		Integer[] repInd = new Integer[]{(ind+1)%size, (ind+2)%size};
//		int predNodeNum1 = ((ind - 1) < 0) ? (size - (Math.abs(ind - 1) % size)) % size : ((ind - 1) % size);
//		int predNodeNum2 = ((ind - 2) < 0) ? (size - (Math.abs(ind - 2) % size)) % size : ((ind - 2) % size);
//
//		Integer[] prevRepInd = new Integer[]{predNodeNum1, predNodeNum2};
//
//		Log.e("Restore", "Restoring from succ : " + repInd[0] + " "+ repInd[1]);
//
//		for (Integer node : repInd) {
//			try {
//				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
//						Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
//				socket.setSoTimeout(1000);
//				Log.e("Restore ", "Restoring from node " + encodedPort.get(nodesConnected.get(node)));
//				DataOutputStream insertStream = new DataOutputStream(socket.getOutputStream());
//
//				insertStream.writeUTF("Restore-");
//
//				Log.e("Restore", "Waiting for cursor");
//				ObjectInputStream ackqueryStream = new ObjectInputStream(socket.getInputStream());
//				List<String> succCur = (ArrayList<String>) ackqueryStream.readObject();
//				Log.e("Restore *", succCur + "");
//				if (succCur.get(0).split("-")[0].equals("Nothing")) {
//					continue;
//				} else {
//					for (int i = 0; i < succCur.size(); i++) {
//						String key = succCur.get(i).split("-")[0];
//						if (checkKey(key, ind)) {
//							cur.addRow(succCur.get(i).split("-"));
//						}
//					}
//				}
//				insertStream.flush();
//				//socket.close();
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			} catch (SocketException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
//
//
//		Log.e("Restore", "Restoring from prev : " + predNodeNum1 + " " + predNodeNum2);
//
//		for (Integer node : prevRepInd) {
//
//			try {
//				Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
//						Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
//				socket.setSoTimeout(1000);
//				Log.e("Restore ", "Restoring from node " + encodedPort.get(nodesConnected.get(node)));
//				DataOutputStream insertStream = new DataOutputStream(socket.getOutputStream());
//
//				insertStream.writeUTF("Restore-");
//
//				Log.e("Restore", "Waiting for cursor");
//				ObjectInputStream ackqueryStream = new ObjectInputStream(socket.getInputStream());
//				List<String> succCur = (ArrayList<String>) ackqueryStream.readObject();
//				Log.e("Restore *", succCur+"");
//				if (succCur.get(0).split("-")[0].equals("Nothing")){
//					continue;
//				} else {
//					for (int i = 0; i < succCur.size(); i++) {
//						String key = succCur.get(i).split("-")[0];
//						if (checkKey(key, node)) {
//							cur.addRow(succCur.get(i).split("-"));
//						}
//					}
//				}
//				insertStream.flush();
//				socket.close();
//
//			} catch (UnknownHostException e) {
//				e.printStackTrace();
//			} catch (SocketException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (ClassNotFoundException e) {
//				e.printStackTrace();
//			}
//		}
//
//		Log.e("RestoreCount", cur.getCount() +"");
//		while(cur.moveToNext()){
//			ContentValues values = new ContentValues();
//			values.put("key", cur.getString(0));
//			values.put("value", cur.getString(1));
//			Log.e("Restoring", values.toString());
//			SQLiteDatabase messenger_db = chordSQLHelper.getWritableDatabase();                         //This creates a Database through which we can save the key, value paris.
//			long _id = messenger_db.replace(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);   //Used to insert into the the table with the content values
//		}
//

		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
        String hashID = null;
        String newSelection = "key = "+"'"+selection+"'";                                              // This is the key which is to be queried in the form of 'key = selection'



        if (selection.equals("@")){
            Log.e("Delete @", "Deleting all rows");
			SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
            messenger_db.rawQuery("Delete from " + FeedReaderContract.FeedEntry.TABLE_NAME, null);
        } else if (selection.equals("*")){
            for (String port : REMOTE_PORTS) {
                try{
                    if (port.equals(portStr)){
						SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
                        messenger_db.rawQuery("Delete from " + FeedReaderContract.FeedEntry.TABLE_NAME, null);
                        Log.e("Deleting *", "Deleting current");
                        continue;
                    }
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                            Integer.parseInt(port) * 2);
                    socket.setSoTimeout(200);
                    DataOutputStream deleteStream = new DataOutputStream(socket.getOutputStream());
                    deleteStream.writeUTF("DeleteAll-" + "All");
//					DataInputStream ackStream = new DataInputStream(socket.getInputStream());
//					ackStream.readUTF();
					deleteStream.flush();
					//socket.close();

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Integer[] nodes = getLocation(selection);
            for (Integer node : nodes){
                try {
                    if (encodedPort.get(nodesConnected.get(node)).equals(portStr)) {
                        Log.e("Delete ", "Deleting from Current");
						SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
                        int d = messenger_db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, newSelection, null);

                        continue;
                    }
					Log.e("Delete ", "Deleted from an other node "+encodedPort.get(nodesConnected.get(node)));
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
                            Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
                    socket.setSoTimeout(200);
                    DataOutputStream queryStream = new DataOutputStream(socket.getOutputStream());
                    queryStream.writeUTF("Delete-" + newSelection);

					queryStream.flush();
//					DataInputStream ackStream = new DataInputStream(socket.getInputStream());
//					ackStream.readUTF();

					//socket.close();


                    //continue;
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                    continue;
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub

//		new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, values.getAsString("key"), values.getAsString("value"));
		String hashID = null;
		try {
			hashID = genHash(values.getAsString("key"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		Log.e("Insert", "In Insert for " + values.getAsString("key") + " " +values.getAsString("value"));
		Integer[] nodes = getLocation(values.getAsString("key"));
		int inserted = 0;
		for (Integer node : nodes) {

			if (encodedPort.get(nodesConnected.get(node)).equals(myPort)){
				Log.e("Insert"," Inserting inside " + values.getAsString("key") + " " +values.getAsString("value") + " " +encodedPort.get(nodesConnected.get(node)));
				SQLiteDatabase messenger_db = chordSQLHelper.getWritableDatabase();                         //This creates a Database through which we can save the key, value paris.
				long _id = messenger_db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);   //Used to insert into the the table with the content values
				Log.e("Insert ", values.toString());
				inserted++;
				continue;
			}
			if(inserted>2) {
				//String msg = "Insert-" + values.getAsString("key") + "-" + values.getAsString("value");
				Log.e("Insert", " Inserting on server " + values.getAsString("key") + " " +values.getAsString("value") + " " + encodedPort.get(nodesConnected.get(node)));
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, values.getAsString("key"), values.getAsString("value"), encodedPort.get(nodesConnected.get(node)));
			} else {
				try {
					Log.e("Insert"," Inserting Outside " +values.getAsString("key") + " " +values.getAsString("value")+ " " + encodedPort.get(nodesConnected.get(node)));
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
							Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
					socket.setSoTimeout(200);
					DataOutputStream insertStream = new DataOutputStream(socket.getOutputStream());
					String msg = "Insert-" + values.getAsString("key") + "-" + values.getAsString("value");
					insertStream.writeUTF(msg);
					insertStream.flush();

					DataInputStream ackStream = new DataInputStream(socket.getInputStream());
					String ack = ackStream.readUTF();

					Log.e("AckInsert",ack);

					inserted++;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					Log.e("Failed","Failed node "+ encodedPort.get(nodesConnected.get(node)));
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					Log.e("Failed","Failed node "+ encodedPort.get(nodesConnected.get(node)));
					e.printStackTrace();
					continue;
				}
			}
		}
		Log.e("HashKey", "Hashed Key: " + hashID);
		return null;
	}


	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
						String[] selectionArgs, String sortOrder) {
		// TODO Auto-generated method stub
		String hashID = null;
		String newSelection = "key = "+"'"+selection+"'";                                              // This is the key which is to be queried in the form of 'key = selection'


		Cursor cursor = null;
		MatrixCursor matCursor = new MatrixCursor(new String[]{"key","value"});

		if (selection.equals("@")){
			Log.e("Query @", "Getting all rows");
			SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
			cursor = messenger_db.rawQuery("select * from "+FeedReaderContract.FeedEntry.TABLE_NAME,null);
			//cursor.moveToNext();
			//Log.e("Query @", "Item " + cursor.getString(0) + cursor.getString(1));
		} else if (selection.equals("*")){
			for (String port : REMOTE_PORTS) {
				try{
					if (port.equals(portStr)){
						SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
						Cursor locCur = messenger_db.rawQuery("select * from " + FeedReaderContract.FeedEntry.TABLE_NAME, null);
						int numRows = locCur.getCount();
						Log.e("Query *", "Current node count : " + numRows);
						while (locCur.moveToNext()) {
							String[] keyValPair = {locCur.getString(0), locCur.getString(1)};
							matCursor.addRow(keyValPair);
						}
						continue;
					}
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
							Integer.parseInt(port) * 2);
					socket.setSoTimeout(200);
					DataOutputStream queryStream = new DataOutputStream(socket.getOutputStream());
					queryStream.writeUTF("QueryAll-" + newSelection);
					queryStream.flush();

					Log.e("Query *", "Waiting for cursor");
					ObjectInputStream ackqueryStream = new ObjectInputStream(socket.getInputStream());
					List<String> succCur = (ArrayList<String>) ackqueryStream.readObject();

					Log.e("Query *", succCur+"");
					for (int i=0; i<succCur.size(); i++){
						matCursor.addRow(succCur.get(i).split("-"));
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			cursor = matCursor;
		} else {
			Integer[] nodes = getLocation(selection);
			Log.e("Querying", "Querying key " + newSelection);
			for (Integer node : nodes){
				try {
					if (encodedPort.get(nodesConnected.get(node)).equals(portStr)) {
						Log.e("Query"," Querying inside " + encodedPort.get(nodesConnected.get(node)));
						SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
						cursor = messenger_db.query(FeedReaderContract.FeedEntry.TABLE_NAME, projection, newSelection, null, null, null, sortOrder);
						Log.e("Query", "Count for "+newSelection+" "+cursor.getCount());
//						if(cursor.getCount()>0) {
//							break;
//						}
						continue;
					}
					Log.e("Query"," Querying "+newSelection + " from " + encodedPort.get(nodesConnected.get(node)));
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
							Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
					socket.setSoTimeout(200);
					DataOutputStream queryStream = new DataOutputStream(socket.getOutputStream());
					queryStream.writeUTF("Query-" + newSelection);
					queryStream.flush();

					DataInputStream ackinsertStream = new DataInputStream(socket.getInputStream());
					String[] values = ackinsertStream.readUTF().split("-");

					//ackinsertStream.close();
					//socket.close();

					if(values.length<2){
						continue;
					}
					matCursor.addRow(values);
					cursor = matCursor;

					Log.e("Query ", "Queryed from "+ encodedPort.get(nodesConnected.get(node)) + " "+  values[0] + " " +values[1]);

					break;
					//continue;
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					Log.e("Error", "Failed AVD : "+ encodedPort.get(nodesConnected.get(node)));
					e.printStackTrace();
					continue;
				} catch (IOException e) {
					Log.e("error", "Failed AVD : " + encodedPort.get(nodesConnected.get(node)));
					e.printStackTrace();
					continue;
				}
			}
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
					  String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}


	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		@Override
		protected Void doInBackground(ServerSocket... serverSockets) {
			ServerSocket serverSocket = serverSockets[0];
			try {
				while (true) {
					Socket clientSocket = serverSocket.accept();
					DataInputStream inStream = new DataInputStream(clientSocket.getInputStream());
					String[] msgReceived = inStream.readUTF().split("-");
					String type = msgReceived[0];
					if (type.equals("Insert")){
						ContentValues values = new ContentValues();
						values.put("key", msgReceived[1]);
						values.put("value", msgReceived[2]);
						SQLiteDatabase messenger_db = chordSQLHelper.getWritableDatabase();                         //This creates a Database through which we can save the key, value paris.

						long _id = messenger_db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);   //Used to insert into the the table with the content values
						Log.e("InsertServ", values.toString());

						DataOutputStream ackStream = new DataOutputStream(clientSocket.getOutputStream());
						ackStream.writeUTF("Insertack");
						ackStream.flush();
					} else if ((type.equals("QueryAll")) || (type.equals("Restore"))){
						Cursor locCur;
						Log.e("Serv", type);
						List<String> rows = new ArrayList<String>();
						SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
						locCur = messenger_db.rawQuery("select * from " + FeedReaderContract.FeedEntry.TABLE_NAME, null);
						if (locCur.getCount()>0) {
							while (locCur.moveToNext()) {
								Log.e("QueryAllServ", "Adding row " + locCur.getString(0) + "-" + locCur.getString(1));
								rows.add(locCur.getString(0) + "-" + locCur.getString(1));
							}
						} else {
							rows.add("Nothing-Nothing");
						}
						ObjectOutputStream objStream = new ObjectOutputStream(clientSocket.getOutputStream());
						objStream.writeObject(rows);
						objStream.flush();
					} else if (type.equals("Query")) {
						String newSelection = msgReceived[1];
						Cursor cursor = null;
						SQLiteDatabase messenger_db = chordSQLHelper.getWritableDatabase();
						Log.e("QueryServ", "Getting Cursor for : "+newSelection);
						cursor = messenger_db.query(FeedReaderContract.FeedEntry.TABLE_NAME, null, newSelection, null, null, null, null);

						Log.e("QueryServ", cursor.getCount()+"");

						if (cursor.getCount() > 0) {
							cursor.moveToFirst();

//						Log.e("QueryServ", "Values is " + cursor.getString(0) + "-" + cursor.getString(1));
							DataOutputStream ackOutStream = new DataOutputStream(clientSocket.getOutputStream());
							ackOutStream.writeUTF(cursor.getString(0) + "-" + cursor.getString(1));
							ackOutStream.flush();
						} else {
							DataOutputStream ackStream = new DataOutputStream(clientSocket.getOutputStream());
							ackStream.writeUTF("Ack-");
							ackStream.flush();
						}
//						Log.e("QueryServ", "Sent value " + cursor.getString(0));
					} else if (type.equals("DeleteAll")) {
						SQLiteDatabase messenger_db = chordSQLHelper.getReadableDatabase();
						messenger_db.rawQuery("Delete from " + FeedReaderContract.FeedEntry.TABLE_NAME, null);
						Log.e("DeleteServ", "Deleting all from Server");
						DataOutputStream ackStream = new DataOutputStream(clientSocket.getOutputStream());
						ackStream.writeUTF("Ack-");
						ackStream.flush();
					} else if (type.equals("Delete")) {
						String newSelection = msgReceived[1];
						Cursor cursor = null;
						SQLiteDatabase messenger_db = chordSQLHelper.getWritableDatabase();
						Log.e("DeleteServ", "Deleting : " + newSelection);
						messenger_db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, newSelection, null);

//						DataOutputStream ackStream = new DataOutputStream(clientSocket.getOutputStream());
//						ackStream.writeUTF("Ack-");
//						ackStream.flush();
					}

					else {
						Log.e("NoValue", "No type named " + type);
					}
//					DataOutputStream ackStream = new DataOutputStream(clientSocket.getOutputStream());
//					ackStream.writeUTF("Ack");
					//clientSocket.close();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

//	private class ClientTask extends AsyncTask<String, Void, Void>{
//
//		@Override
//		protected Void doInBackground(String... msgs) {
//
//			String key = msgs[0];
//			String val = msgs[1];
//			ContentValues values = new ContentValues();
//			values.put("key",key);
//			values.put("value", val);
//			String hashID = null;
//			try {
//				hashID = genHash(key);
//			} catch (NoSuchAlgorithmException e) {
//				e.printStackTrace();
//			}
//			Integer[] nodes = getLocation(key);
//
//				for (Integer node : nodes) {
//					try {
//					if (encodedPort.get(nodesConnected.get(node)).equals(portStr)){
//						SQLiteDatabase messenger_db = chordSQLHelper.getWritableDatabase();                         //This creates a Database through which we can save the key, value paris.
//						long _id = messenger_db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);   //Used to insert into the the table with the content values
//						Log.e("Insert ", key + val);
//						continue;
//					}
//					String msg = "Insert-" + key + "-" + val;
//
//
//					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
//							Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
//					socket.setSoTimeout(200);
//					Log.e("Insert ", "Inserting to node " + encodedPort.get(nodesConnected.get(node)) + key);
//					DataOutputStream insertStream = new DataOutputStream(socket.getOutputStream());
//					insertStream.writeUTF(msg);
//					insertStream.flush();
//
//					DataInputStream ackStream = new DataInputStream(socket.getInputStream());
//					String ack = ackStream.readUTF();
//					Log.e("AckInsert",ack);
//					//socket.close();
//
//					} catch (UnknownHostException e) {
//						e.printStackTrace();
//					} catch (SocketException e) {
//						Log.e("FailedSocket", "Failed node " + node);
//						e.printStackTrace();
//						continue;
//					} catch (IOException e) {
//						Log.e("FailedIO", "Failed node " + node);
//						e.printStackTrace();
//						continue;
//					}
//
//				}
//
//			Log.e("HashKey", "Hashed Key: " + hashID);
//
//			return null;
//		}
//	}

	private class ClientTask extends AsyncTask<String, Void, Void>{

		@Override
		protected Void doInBackground(String... msgs) {

			String key = msgs[0];
			String val = msgs[1];
			String node = msgs[2];
			ContentValues values = new ContentValues();
			values.put("key",key);
			values.put("value", val);
			String hashID = null;
			try {
				hashID = genHash(key);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
			//Integer[] nodes = getLocation(key);


				try {

					String msg = "Insert-" + key + "-" + val;


					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
							Integer.parseInt(node) * 2);
					socket.setSoTimeout(200);
					Log.e("Insert ", "Inserting to node " + node + key);
					DataOutputStream insertStream = new DataOutputStream(socket.getOutputStream());
					insertStream.writeUTF(msg);
					insertStream.flush();

					DataInputStream ackStream = new DataInputStream(socket.getInputStream());
					String ack = ackStream.readUTF();
					Log.e("AckInsert",ack);
					//socket.close();

				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (SocketException e) {
					Log.e("FailedSocket", "Failed node " + node);
					e.printStackTrace();
					//continue;
				} catch (IOException e) {
					Log.e("FailedIO", "Failed node " + node);
					e.printStackTrace();
					//continue;
				}



			Log.e("HashKey", "Hashed Key: " + hashID);

			return null;
		}
	}
	private class ClientTaskRestore extends AsyncTask<String, Void, Void>{

		@Override
		protected Void doInBackground(String... msgs) {

			if (msgs[0].equals("Restore")){
				Log.e("Restore", "Restoring the avd");
			}
			delete(mUri, "@",null);
			MatrixCursor cur = new MatrixCursor(new String[]{"key","value"});
			Integer ind = null;
			try {
				ind = nodesConnected.indexOf(genHash(portStr));
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}

			Integer[] repInd = new Integer[]{(ind+1)%size, (ind+2)%size};
			int predNodeNum1 = ((ind - 1) < 0) ? (size - (Math.abs(ind - 1) % size)) % size : ((ind - 1) % size);
			int predNodeNum2 = ((ind - 2) < 0) ? (size - (Math.abs(ind - 2) % size)) % size : ((ind - 2) % size);

			Integer[] prevRepInd = new Integer[]{predNodeNum1, predNodeNum2};

			Log.e("Restore", "Restoring from succ : " + repInd[0] + " "+ repInd[1]);

				for (Integer node : repInd) {
					try {
					Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
							Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
					socket.setSoTimeout(200);
					Log.e("Restore ", "Restoring from node " + encodedPort.get(nodesConnected.get(node)));
					DataOutputStream insertStream = new DataOutputStream(socket.getOutputStream());

					insertStream.writeUTF("Restore-");

					Log.e("Restore", "Waiting for cursor");
					ObjectInputStream ackqueryStream = new ObjectInputStream(socket.getInputStream());
					List<String> succCur = (ArrayList<String>) ackqueryStream.readObject();
					insertStream.flush();
					Log.e("Restore *", succCur + "");
					if (succCur.get(0).split("-")[0].equals("Nothing")) {
						continue;
					} else {
						for (int i = 0; i < succCur.size(); i++) {
							String key = succCur.get(i).split("-")[0];
							if (checkKey(key, ind)) {
								cur.addRow(succCur.get(i).split("-"));
							}
						}
					}

					//socket.close();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}


				Log.e("Restore", "Restoring from prev : " + predNodeNum1 + " " + predNodeNum2);

				for (Integer node : prevRepInd) {

					try {
						Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),      //Creating a socket to deliver the message.
								Integer.parseInt(encodedPort.get(nodesConnected.get(node))) * 2);
						socket.setSoTimeout(200);
						Log.e("Restore ", "Restoring from node " + encodedPort.get(nodesConnected.get(node)));
						DataOutputStream insertStream = new DataOutputStream(socket.getOutputStream());

						insertStream.writeUTF("Restore-");

						Log.e("Restore", "Waiting for cursor");
						ObjectInputStream ackqueryStream = new ObjectInputStream(socket.getInputStream());
						List<String> succCur = (ArrayList<String>) ackqueryStream.readObject();
						insertStream.flush();
						Log.e("Restore *", succCur+"");
						if (succCur.get(0).split("-")[0].equals("Nothing")){
							continue;
						} else {
							for (int i = 0; i < succCur.size(); i++) {
								String key = succCur.get(i).split("-")[0];
								if (checkKey(key, node)) {
									cur.addRow(succCur.get(i).split("-"));
								}
							}
						}

						//socket.close();

					} catch (UnknownHostException e) {
						e.printStackTrace();
					} catch (SocketException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}

				Log.e("RestoreCount", cur.getCount() +"");
				while(cur.moveToNext()){
					ContentValues values = new ContentValues();
					values.put("key", cur.getString(0));
					values.put("value", cur.getString(1));
					Log.e("Restoring", values.toString());
					SQLiteDatabase messenger_db = chordSQLHelper.getWritableDatabase();                         //This creates a Database through which we can save the key, value paris.
					long _id = messenger_db.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);   //Used to insert into the the table with the content values
				}


			return null;
		}
	}
	protected String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

	private Boolean checkKey(String key, Integer ind) {
		try {

			String hashID = genHash(key);


			int predNodeNum = ((ind- 1) < 0) ? (size - (Math.abs(ind - 1) % size)) % size : ((ind - 1) % size);
			//int succNodeNum = (i + 1) % size;
//			Log.e("Checking", predNodeNum+"");
//			Log.e("Checking", nodesConnected.get(predNodeNum)+"");
//			Log.e("Checking", "index " + ind + " ");
//			Log.e("Checking", nodesConnected.get(ind)+"");
			if (nodesConnected.get(predNodeNum).compareTo(nodesConnected.get(ind)) < 0) {
				if ((hashID.compareTo(nodesConnected.get(predNodeNum)) > 0) && (hashID.compareTo(nodesConnected.get(ind)) <= 0)) {
					return true;
				}
			} else {
				if (((hashID.compareTo(nodesConnected.get(predNodeNum)) > 0) && hashID.compareTo(nodesConnected.get(ind)) > 0) || ((hashID.compareTo(nodesConnected.get(predNodeNum)) < 0) && hashID.compareTo(nodesConnected.get(ind)) < 0))
					return true;

			}

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return false;
	}
	private Integer[] getLocation(String key) {

		try {

			String hashID = genHash(key);

			for (int i = 0; i < nodesConnected.size(); i++) {
				int predNodeNum = ((i - 1) < 0) ? (size - (Math.abs(i - 1) % size)) % size : ((i - 1) % size);
				//int succNodeNum = (i + 1) % size;
				if (nodesConnected.get(predNodeNum).compareTo(nodesConnected.get(i)) < 0) {
					if ((hashID.compareTo(nodesConnected.get(predNodeNum)) > 0) && (hashID.compareTo(nodesConnected.get(i)) <= 0)) {
						Integer[] nodes = {i, (i + 1) % size, (i + 2) % size};
						return nodes;
					}
				} else {
					if (((hashID.compareTo(nodesConnected.get(predNodeNum)) > 0) && hashID.compareTo(nodesConnected.get(i)) > 0) || ((hashID.compareTo(nodesConnected.get(predNodeNum)) < 0) && hashID.compareTo(nodesConnected.get(i)) < 0))
					{
						Integer[] nodes = {i, (i + 1) % size, (i + 2) % size};
						return nodes;
					}
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		Integer[] nodes = {0, 0, 0, 0, 0, 0};
		return nodes;
	}
}

/* References:
1. https://beginnersbook.com/2013/12/java-arraylist-get-method-example/
 */