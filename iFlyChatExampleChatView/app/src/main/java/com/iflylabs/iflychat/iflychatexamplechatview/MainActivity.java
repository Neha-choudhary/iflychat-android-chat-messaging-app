package com.iflylabs.iflychat.iflychatexamplechatview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.iflylabs.iFlyChatLibrary.iFlyChatConfig;
import com.iflylabs.iFlyChatLibrary.iFlyChatMessage;
import com.iflylabs.iFlyChatLibrary.iFlyChatService;
import com.iflylabs.iFlyChatLibrary.iFlyChatUser;
import com.iflylabs.iFlyChatLibrary.iFlyChatUserAuthService;
import com.iflylabs.iFlyChatLibrary.iFlyChatUserSession;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    String USERID = "";
    String ROOMID = "0";
    String SESSIONKEY = "";
    String SERVERHOST = "api.iflychat.com";
    String AUTHURL = "http://your.website.com/auth-url";
    String USERNAME = "username";
    String PASSWORD = "password";


    BroadcastReceiver receiver;

    RelativeLayout top_bar;
    TextView chat_status;
    ListView messageListView;
    ImageButton sendMessage;
    EditText messageText;
    List<iFlyChatMessage> messageList;
    MessageAdapter messageAdapter;

    public iFlyChatUser loggedUser;
    iFlyChatMessage receivedMessage;

    SharedPreferences loginSession;

    iFlyChatUserSession session;
    iFlyChatConfig config;
    iFlyChatUserAuthService authService;
    iFlyChatService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!ROOMID.equals(""))
        {
            getSupportActionBar().setTitle("Public Chatroom");
        }
        else
        {
            getSupportActionBar().setTitle("User");
        }

        messageListView = (ListView)findViewById(R.id.message_list);
        sendMessage = (ImageButton)findViewById(R.id.send_message);
        messageText = (EditText)findViewById(R.id.write_message);

        top_bar = (RelativeLayout)findViewById(R.id.top_bar);
        chat_status = (TextView)findViewById(R.id.chat_status);

        top_bar.setBackgroundColor(Color.RED);
        chat_status.setBackgroundColor(Color.RED);
        chat_status.setText("Not connected");

        session = new iFlyChatUserSession(USERNAME,PASSWORD,SESSIONKEY);

        config = new iFlyChatConfig(SERVERHOST,AUTHURL,false,session);

        authService = new iFlyChatUserAuthService(config,session,getApplicationContext());

        service = new iFlyChatService(session, config, authService, getApplicationContext());

        service.connectChat(session.getSessionKey());

        top_bar.setBackgroundColor(Color.YELLOW);
        chat_status.setBackgroundColor(Color.YELLOW);
        chat_status.setText("Connecting...");


        loginSession = getApplicationContext().getSharedPreferences("loginSession",0);

        final SharedPreferences.Editor editor = loginSession.edit();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //check if the message is from user or room
                if(intent.getAction().equals("iFlyChat.onMessageFromRoom"))
                {
                    Log.e("Received","message received");

                    if(!ROOMID.equals(""))
                    {
                        receivedMessage = intent.getParcelableExtra("messageObj");
                        if (receivedMessage.getToId().equals(ROOMID))
                        {
                            messageReceived(receivedMessage);
                        }
                    }
                }
                else if(intent.getAction().equals("iFlyChat.onChatConnect"))
                {
                    loggedUser = intent.getParcelableExtra("currentUser");
                    editor.putString("uid",loggedUser.getId());
                    editor.putString("name",loggedUser.getName());

                    editor.apply();

                    top_bar.setBackgroundColor(Color.GREEN);
                    chat_status.setBackgroundColor(Color.GREEN);
                    chat_status.setText("Connected");
                }
                else if(intent.getAction().equals("iFlyChat.onChatDisconnect"))
                {
                    top_bar.setBackgroundColor(Color.RED);
                    chat_status.setBackgroundColor(Color.RED);
                    chat_status.setText("Not connected");
                }
                else if(intent.getAction().equals("iFlyChat.onMessageFromUser"))
                {
                    Log.e("Received","message received");

                    if(!USERID.equals(""))
                    {
                        receivedMessage = intent.getParcelableExtra("messageObj");
                        if (receivedMessage.getToId().equals(USERID) || receivedMessage.getToId().equals(loggedUser.getId()))
                        {
                            messageReceived(receivedMessage);
                        }
                    }
                }
            }
        };

        sendMessage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                sendMessage();
                messageText.setText("");


            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void messageReceived(iFlyChatMessage message)
    {
        if(messageAdapter == null)
        {
            if(messageList == null)
            {
                messageList = new ArrayList<iFlyChatMessage>();
            }
            messageList.add(message);
            bindData();
        }
        else
        {
            messageList.add(message);
            messageAdapter.notifyDataSetChanged();
        }
    }

    public void bindData()
    {
        messageAdapter = new MessageAdapter(messageList, getApplicationContext());

        messageListView.setAdapter(messageAdapter);
    }

    public void sendMessage()
    {
        String toID = "";
        String toName = "";
        if(!messageText.getText().toString().isEmpty())
        {
            if(!ROOMID.equals(""))
            {
                toID = ROOMID;
                toName = "Public Chatroom";
                iFlyChatMessage sendMessage = new iFlyChatMessage("",loggedUser.getId(),toID,USERNAME,toName,messageText.getText().toString(),"",loggedUser.getProfileUrl(),loggedUser.getAvatarUrl(),loggedUser.getRole(),"");
                service.sendMessageToRoom(sendMessage);

            }
            else if(!USERID.equals(""))
            {
                toID = USERID;
                toName = "User"+ " " +toID;
                iFlyChatMessage sendMessage = new iFlyChatMessage("",loggedUser.getId(),toID,USERNAME,toName,messageText.getText().toString(),"",loggedUser.getProfileUrl(),loggedUser.getAvatarUrl(),loggedUser.getRole(),"");
                service.sendMessageToUser(sendMessage);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter("iFlyChat.onChatConnect"));
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter("iFlyChat.onChatDisconnect"));
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter("iFlyChat.onMessageFromRoom"));
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter("iFlyChat.onMessageFromUser"));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        service.diconnectChat();
        super.onDestroy();
    }
}
