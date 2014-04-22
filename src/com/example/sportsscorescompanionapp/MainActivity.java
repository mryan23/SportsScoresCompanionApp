package com.example.sportsscorescompanionapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.PebbleKit.PebbleAckReceiver;
import com.getpebble.android.kit.PebbleKit.PebbleNackReceiver;
import com.getpebble.android.kit.util.PebbleDictionary;

public class MainActivity extends Activity {

	/**
	 * The PebbleDictionary key value used to retrieve the identifying
	 * transactionId field.
	 */
	private static final int EVENT_ID_KEY = 79;

	/**
	 * different values stored with the EVENT_ID_KEY
	 */
	private static final int SCORES_LIST_REQUEST = 94;
	private static final int SCORES_LIST_SEND = 95;
	private static final int GROCERY_LIST_CHECK_RECEIVE = 96;
	private static final int GROCERY_LIST_CHECK_SEND = 97;

	// ~Constants---------------------------------------------------------------------------------------------------------------------
	/**
	 * String used to identify Log messages coming from this class
	 */
	private static final String TAG = "GROCERY_ACTIVITY";
	/**
	 * The UUID of the Pebble APP running on the Pebble.
	 */
	private static final UUID PEBBLE_APP_UUID = UUID
			.fromString("798b750a-24c2-43fb-a8c5-b14f0b1e925f");

	private static final int NUMBER_OF_GAMES_KEY = 35;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setupPebbleCommunication();

	}

	private void setupPebbleCommunication() {
		// TODO Auto-generated method stub

		// Check if Pebble is already connected!
		if (PebbleKit.isWatchConnected(getApplicationContext())) {

			String messageString = "Your Pebble watch is connected!";
			Toast.makeText(this, messageString, Toast.LENGTH_SHORT).show();
			Log.i(TAG, messageString);
		} else {

			String messageString = "Your Pebble watch IS NOT connected! FIX THIS!";
			Toast.makeText(this, messageString, Toast.LENGTH_SHORT).show();
			Log.i(TAG, messageString);
		}

		// Listen for the pebble connection event
		PebbleKit.registerPebbleConnectedReceiver(this,
				new BroadcastReceiver() {

					@Override
					public void onReceive(Context context, Intent intent) {

						String messageString = "You just connected your Pebble Watch! Congrats!";
						Toast.makeText(context, messageString,
								Toast.LENGTH_SHORT).show();
						Log.i(TAG, messageString);
					}
				});

		// Listen for the pebble disconnected event
		PebbleKit.registerPebbleConnectedReceiver(this,
				new BroadcastReceiver() {

					@Override
					public void onReceive(Context context, Intent intent) {

						String messageString = "You just disconnected your Pebble Watch! Why you disconnect!??";
						Toast.makeText(context, messageString,
								Toast.LENGTH_SHORT).show();
						Log.i(TAG, messageString);
					}
				});

		// Register to receive ACKS back from Pebble after sending message
		PebbleKit.registerReceivedAckHandler(this, new PebbleAckReceiver(
				PEBBLE_APP_UUID) {

			@Override
			public void receiveAck(Context context, int transactionId) {

				String messageString = "RECEIVED AN ACK FOR transactionId: "
						+ transactionId;
				Toast.makeText(context, messageString, Toast.LENGTH_SHORT)
						.show();
				Log.i(TAG, messageString);
			}
		});

		// Register to receive NACKS back from the Pebble after sending message
		PebbleKit.registerReceivedNackHandler(this, new PebbleNackReceiver(
				PEBBLE_APP_UUID) {

			@Override
			public void receiveNack(Context context, int transactionId) {

				String messageString = "RECEIVED A NACK FOR transactionId: "
						+ transactionId;
				Toast.makeText(context, messageString, Toast.LENGTH_SHORT)
						.show();
				Log.i(TAG, messageString);
			}
		});

		// Register to receive messages
		PebbleKit.registerReceivedDataHandler(this,
				new PebbleKit.PebbleDataReceiver(PEBBLE_APP_UUID) {

					@Override
					public void receiveData(Context context, int transactionId,
							PebbleDictionary data) {
						PebbleKit.sendAckToPebble(context, transactionId);
						int myTransactionId = Long.valueOf(
								data.getInteger(NUMBER_OF_GAMES_KEY))
								.intValue();
						String messageString = "Received a message from the Pebble with myTransactionId == "
								+ myTransactionId;
						Toast.makeText(context, messageString,
								Toast.LENGTH_SHORT).show();

						switch (myTransactionId) {
						case SCORES_LIST_REQUEST:
							GetScoresTask gst = new GetScoresTask();
							gst.execute();
							break;

						}
						/*
						 * PebbleKit.sendAckToPebble(context, transactionId);
						 * 
						 * int myTransactionId = Long.valueOf(
						 * data.getInteger(EVENT_ID_KEY)).intValue();
						 * 
						 * String messageString =
						 * "Received a message from the Pebble with myTransactionId == "
						 * + myTransactionId; Toast.makeText(context,
						 * messageString, Toast.LENGTH_SHORT).show(); Log.i(TAG,
						 * messageString);
						 * 
						 * switch (myTransactionId) {
						 * 
						 * case GROCERY_LIST_CHECK_RECEIVE:
						 * 
						 * setCheckFromPebble(data.getInteger(CHECK_KEY)
						 * .intValue()); break;
						 * 
						 * case GROCERY_LIST_REQUEST: sendGroceryListToPebble();
						 * break;
						 * 
						 * default: Log.e(TAG,
						 * "Received an unknown transactionId: " +
						 * myTransactionId); break; }
						 */

					}
				});

	}

	/**
	 * Sends the scores to the pebble
	 */
	private void sendScoresToPebble(ArrayList<Game> games) {

		PebbleDictionary scoresDict = new PebbleDictionary();
		byte[] bytes;
		byte[] tempBytes;
		for(int i = 1; i < games.size();i++){
			games.remove(i);
		}

		scoresDict.addUint32(NUMBER_OF_GAMES_KEY, games.size());

		for (int i = 0; i < games.size(); i++) {
			Game game = games.get(i);
			String str = game.home + " " + game.homeScore + "\n" + game.visitor
					+ " " + game.visitorScore;

			bytes = str.getBytes();
			scoresDict.addBytes(i, bytes);
		}

		PebbleKit.sendDataToPebbleWithTransactionId(this, PEBBLE_APP_UUID,
				scoresDict, SCORES_LIST_SEND);
		putScoresOnScreen(games);

		// Add all items from the list to a PebbleDictionary
		/*
		 * PebbleDictionary groceryListDict = new PebbleDictionary(); CheckBox
		 * checkBox; byte[] bytes; byte[] tempBytes; int j;
		 * 
		 * // Store list size in a PebbleDictionary
		 * groceryListDict.addUint32(LIST_SIZE_KEY, groceryList.size()); // Add
		 * all items from the list to a PebbleDictionary for (int i = 0; i <
		 * groceryList.size(); i++) {
		 * 
		 * Log.i(TAG, "groceryListView child shizzle " +
		 * groceryListView.getChildAt(i)); checkBox = (CheckBox)
		 * adapter.getView(i, null, groceryListView)
		 * .findViewById(R.id.checkBox1); // checkBox = (CheckBox) //
		 * groceryListView.getChildAt(i).findViewById(R.id.checkBox1);
		 * 
		 * // Store each item bytes = new
		 * byte[groceryList.get(i).getBytes().length + 1]; tempBytes =
		 * groceryList.get(i).getBytes(); for (j = 0; j < bytes.length - 1; j++)
		 * {
		 * 
		 * bytes[j] = tempBytes[j]; } // Store the status of each item's
		 * checkbox bytes[j] = (checkBox.isChecked()) ? (byte) (1 & 0xFF) :
		 * (byte) (0 & 0xFF);
		 * 
		 * groceryListDict.addBytes(i, bytes); }
		 * 
		 * // Send the PebbleDictionary to the Pebble Watch app with //
		 * PEBBLE_APP_UUID with the appropriate TransactionId
		 * PebbleKit.sendDataToPebbleWithTransactionId(this, PEBBLE_APP_UUID,
		 * groceryListDict, GROCERY_LIST_SEND); Log.i(TAG,
		 * "Grocery list to Pebble.......SENT!!!!!!!!!!!!!!!!!!!!");
		 */

	}

	private void putScoresOnScreen(ArrayList<Game> games) {
		// TODO Auto-generated method stub
		TextView tv = (TextView)findViewById(R.id.textView1);
		String text="";
		
		for(Game game: games){
			text+=game.home + " " + game.homeScore + "\n" + game.visitor
					+ " " + game.visitorScore+"\n\n";
		}
		
		tv.setText(text);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private class GetScoresTask extends AsyncTask<Void, Void, ArrayList<Game>> {

		@Override
		protected ArrayList<Game> doInBackground(Void... arg0) {

			ArrayList<Game> games = new ArrayList<Game>();
			// String
			// urlToRead="http://api.sportsdatallc.org/mlb-t4/daily/boxscore/2014/04/20.xml?api_key=63pnr9zh2kh92ee2myjkawxe";
			String urlToRead = "http://api.sportsdatallc.org/mlb-t4/daily/boxscore/";

			Calendar cal = Calendar.getInstance();
			urlToRead += cal.get(Calendar.YEAR) + "/"
					+ String.format("%02d", cal.get(Calendar.MONTH) + 1) + "/"
					+ String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));

			// add dates
			urlToRead += ".xml?api_key=63pnr9zh2kh92ee2myjkawxe";
			Log.d("URL", urlToRead);
			URL url;
			HttpURLConnection conn;
			BufferedReader rd;
			String line;
			String result = "";
			try {
				url = new URL(urlToRead);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				rd = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				while ((line = rd.readLine()) != null) {
					result += line;
				}
				rd.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder db = dbFactory.newDocumentBuilder();
				InputSource is = new InputSource();
				is.setCharacterStream(new StringReader(result));
				Document doc = db.parse(is);

				NodeList nList = doc.getElementsByTagName("boxscore");
				for (int i = 0; i < nList.getLength(); i++) {
					Node nNode = nList.item(i);
					Element element = (Element) nNode;
					Element visitor = (Element) element.getElementsByTagName(
							"visitor").item(0);
					Log.d(visitor.getAttribute("abbr"),
							visitor.getAttribute("runs"));
					Element home = (Element) element.getElementsByTagName(
							"home").item(0);
					Log.d(home.getAttribute("abbr"), home.getAttribute("runs"));
					Game game = new Game();
					game.home = home.getAttribute("abbr");
					game.visitor = visitor.getAttribute("abbr");
					String homeScore = home.getAttribute("runs");
					if (!homeScore.equals(""))
						game.homeScore = Integer.parseInt(homeScore);
					else
						game.visitorScore = 0;
					String visitorScore = visitor.getAttribute("runs");
					if (!visitorScore.equals(""))
						game.visitorScore = Integer.parseInt(visitorScore);
					else
						game.visitorScore = 0;
					games.add(game);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// TODO Auto-generated method stub
			return games;
		}

		@Override
		protected void onPostExecute(ArrayList<Game> games) {
			int j = 7;
			j += games.size();
			sendScoresToPebble(games);
		}

	}

}
