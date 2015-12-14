package com.iflylabs.iflychat.iflychatexamplechatview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.iflylabs.iFlyChatLibrary.iFlyChatConfig;
import com.iflylabs.iFlyChatLibrary.iFlyChatMessage;
import com.iflylabs.iFlyChatLibrary.iFlyChatRoster;
import com.iflylabs.iFlyChatLibrary.iFlyChatService;
import com.iflylabs.iFlyChatLibrary.iFlyChatUser;
import com.iflylabs.iFlyChatLibrary.iFlyChatUserAuthService;
import com.iflylabs.iFlyChatLibrary.iFlyChatUserSession;
import com.iflylabs.iFlyChatLibrary.util.iFlyChatUtilities;
import com.iflylabs.iflychatexamplechatview.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;



public class MainActivity extends AppCompatActivity {

    String USERID = "";
    String ROOMID = "";
    String TONAME_ROOM = "";
    String TONAME_USER = "";
    String SESSIONKEY = "";
    String SERVERHOST = "";
    String AUTHURL = "";
    String USERNAME = "";
    String PASSWORD = "";
    String avatarUrl = "";
    // UI Variables
    private Toolbar toolbar;
    ImageView userImg;
    CircularImageView roomImg;
    ProgressBar progressBar;
    BroadcastReceiver receiver;
    LocalBroadcastManager bManager;
    RelativeLayout top_bar, sendAttachmentView, take_photo_layout, take_video_layout, select_file_layout, record_sound_layout;
    TextView chat_status;
    ListView messageListView;
    ImageButton sendMessage, sendAttachment;
    EditText messageText;
    private Uri fileUri;
    private int attachmentOrTextCount=0, threadHistoryCount =0, threadHistoryArrayCount=-1, index=0, top=0,
            emptyRoomThreadHistoryFlag=0, emptyUserThreadHistoryFlag=0;

    private static final int TAKE_PHOTO_REQUEST_CODE = 100;
    private static final int TAKE_VIDEO__REQUEST_CODE = 200;
    private static final int SELECT_FILE_REQUEST_CODE = 300;
    private static final int RECORD_SOUND_REQUEST_CODE = 400;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    boolean defaultUserImageFlag = false;
    private TextDrawable.IBuilder mDrawableBuilder = TextDrawable.builder().round();
    private ColorGenerator mColorGenerator = ColorGenerator.MATERIAL;

    private HashMap<String ,String> chatSettings;
    LinkedHashMap<String,iFlyChatMessage> currentMessageMap = new LinkedHashMap<String, iFlyChatMessage>();
    LinkedHashMap<String,LinkedHashMap<String,iFlyChatMessage>> userMessageMap = new LinkedHashMap<String,LinkedHashMap<String,iFlyChatMessage>>();
    LinkedHashMap<String,LinkedHashMap<String,iFlyChatMessage>> roomMessageMap = new LinkedHashMap<String,LinkedHashMap<String,iFlyChatMessage>>();
    List<iFlyChatMessage> messageList;
    MessageAdapter messageAdapter;
    int roomCount=0, userCount=0;
    public iFlyChatUser loggedUser;
    iFlyChatMessage receivedMessage;
    SharedPreferences loginSession;
    iFlyChatUserSession session;
    iFlyChatConfig config;
    iFlyChatUserAuthService authService;
    iFlyChatService service;
    private iFlyChatRoster roster;
    private HashMap<String,iFlyChatUser> users= new HashMap<String,iFlyChatUser>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bManager = LocalBroadcastManager.getInstance(this);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        roomImg = (CircularImageView) findViewById(R.id.default_room);
        userImg = (ImageView) findViewById(R.id.default_user);
        toolbar = (Toolbar) findViewById(R.id.user_layout_toolbar);
        setSupportActionBar(toolbar);

        if(!ROOMID.equals(""))
        {

            final TextView textViewToChange = (TextView) findViewById(R.id.user_layout_inputSearch);
            textViewToChange.setText(TONAME_ROOM);

            roomImg.setVisibility(View.VISIBLE);
            userImg.setVisibility(View.GONE);

            Drawable placeHolder = ContextCompat.getDrawable(getApplicationContext(), R.drawable.home);
            roomImg.setImageDrawable(placeHolder);
            roomImg.setBorderColor(mColorGenerator.getColor(TONAME_ROOM));

            if(!roomMessageMap.containsKey(ROOMID))
                roomMessageMap.put(ROOMID,currentMessageMap);
            else {
                currentMessageMap=roomMessageMap.get(ROOMID);
            }

        }
        else
        {

            final TextView textViewToChange = (TextView) findViewById(R.id.user_layout_inputSearch);
            textViewToChange.setText(TONAME_USER);

            if(!userMessageMap.containsKey(USERID))
                userMessageMap.put(USERID,currentMessageMap);
            else {
                currentMessageMap=userMessageMap.get(USERID);
            }



        }

        messageListView = (ListView)findViewById(R.id.message_list);
        sendMessage = (ImageButton)findViewById(R.id.send_message);
        sendAttachment = (ImageButton) findViewById(R.id.attachment);

        sendAttachmentView = (RelativeLayout) findViewById(R.id.attachment_layout);
        take_photo_layout  =(RelativeLayout) findViewById(R.id.take_photo_layout);
        take_video_layout = (RelativeLayout) findViewById(R.id.take_video_layout);
        select_file_layout = (RelativeLayout) findViewById(R.id.select_file_layout);
        record_sound_layout = (RelativeLayout)findViewById(R.id.record_sound_layout);

        messageText = (EditText)findViewById(R.id.write_message);
        top_bar = (RelativeLayout)findViewById(R.id.top_bar);
        chat_status = (TextView)findViewById(R.id.chat_status);

        top_bar.setBackgroundColor(Color.RED);
        chat_status.setBackgroundColor(Color.RED);
        chat_status.setText("Not connected");
        //iFlyChat Library objects

        iFlyChatUtilities.setiFlyChatContext(getApplicationContext());
        iFlyChatUtilities.setIsDebug(true);


        session = new iFlyChatUserSession(USERNAME,PASSWORD,SESSIONKEY);

        config = new iFlyChatConfig(SERVERHOST, AUTHURL, false);
        config.setAutoReconnect(true);

        authService = new iFlyChatUserAuthService(config,session);
        SESSIONKEY = session.getSessionKey();
        config.getIflychatSettings(SESSIONKEY);
        chatSettings = config.getChatSettings();

        service = new iFlyChatService(session, config, authService);
        top_bar.setBackgroundColor(Color.YELLOW);
        chat_status.setBackgroundColor(Color.YELLOW);
        chat_status.setText("Connecting to iFlyChat Server");
        service.connectChat(SESSIONKEY);


        loginSession = getApplicationContext().getSharedPreferences("loginSession",0);

        final SharedPreferences.Editor editor = loginSession.edit();

        messageListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0) {
                    // check if we reached the top of the list
                    index = messageListView.getFirstVisiblePosition();
                    View v = messageListView.getChildAt(0);
                    top = (v == null) ? 0 : v.getTop();
                    if (top == 0 && threadHistoryCount == 1) {

                        if (messageList != null) {
                            // reached the top:
                            iFlyChatMessage message = messageList.get(0);
                            if (!ROOMID.equals("")) {
                                if (emptyRoomThreadHistoryFlag != 1)
                                    service.getRoomThreadHistory(ROOMID, TONAME_ROOM, message.getMessageId());
                            } else {
                                if (emptyUserThreadHistoryFlag != 1)
                                    service.getUserThreadHistory(USERID, TONAME_USER, message.getMessageId());
                            }
                            return;
                        }
                    }
                }
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {


                if(intent.getAction().equals("iFlyChat.onChatConnect"))
                {
                    loggedUser = intent.getParcelableExtra("currentUser");
                    editor.putString("uid",loggedUser.getId());
                    editor.putString("name", loggedUser.getName());

                    editor.apply();

                    top_bar.setBackgroundColor(Color.parseColor("#00D948"));
                    chat_status.setBackgroundColor(Color.parseColor("#00D948"));
                    chat_status.setText("Connected");
                    chat_status.setText(" ");
                    top_bar.getLayoutParams().height= 5;
                    top_bar.requestLayout();

                    if(!USERID.equals(""))
                        service.getUserThreadHistory(USERID, TONAME_USER, "");
                    else
                        service.getRoomThreadHistory(ROOMID, TONAME_ROOM,"");

                }
                else if(intent.getAction().equals("iFlyChat.onGlobalListUpdate")){
// Get the roster object, then get users list from this object.
// USERID is there in these users, get its avatar url and download it.

                    roster = intent.getParcelableExtra("globalList");
                    users = roster.getUserList();
                    for (Map.Entry<String, iFlyChatUser> entry : users.entrySet()) {

                        iFlyChatUser userObj = entry.getValue();
                        if(userObj.getId().equals(USERID))
                        {
                            avatarUrl = userObj.getAvatarUrl();
                            setChatImage(getApplicationContext(), userImg, roomImg, USERID, TONAME_USER, avatarUrl);
                        }

                    }


                }

                else if(intent.getAction().equals("iFlyChat.onMessageFromUser"))
                {

                    if(!USERID.equals(""))
                    {
                        receivedMessage = intent.getParcelableExtra("messageObj");
                        // only messages from USERID user to loggedUser should be updated in the UI.
                        if (receivedMessage.getFromId().equals(USERID) && receivedMessage.getToId().equals(loggedUser.getId()))
                        {
                                messageReceived(receivedMessage);
                        }
                        if(receivedMessage.getFromId().equals(loggedUser.getId()) && receivedMessage.getToId().equals(USERID))
                        {
                            if(!currentMessageMap.containsKey(receivedMessage.getMessageId()))
                            {
                                messageReceived(receivedMessage);
                            }

                        }
                    }
                }

                else if(intent.getAction().equals("iFlyChat.onMessageFromRoom"))
                {


                    if(!ROOMID.equals(""))
                    {
                        receivedMessage = intent.getParcelableExtra("messageObj");
                        if (!(receivedMessage.getFromId().equals(loggedUser.getId())) && receivedMessage.getToId().equals(ROOMID))
                        {
                            messageReceived(receivedMessage);

                        }

                        if(receivedMessage.getFromId().equals(loggedUser.getId()) && receivedMessage.getToId().equals(ROOMID))
                        {
                            if(!currentMessageMap.containsKey(receivedMessage.getMessageId()))
                            {
                                messageReceived(receivedMessage);
                            }

                        }
                    }
                }
                else if (intent.getAction().equals("iFlyChat.onUserThreadHistory")){


                    String userId = intent.getStringExtra("forUserId");

                    if(!USERID.equals("") && USERID.equals(userId)){



                        LinkedHashMap<String, iFlyChatMessage> threadHistoryMessageMap = (LinkedHashMap<String, iFlyChatMessage>) intent.getSerializableExtra("threadHistoryMap");
                        if(threadHistoryMessageMap!=null && threadHistoryMessageMap.size()>0){
                            int i=0;
                            for (Map.Entry<String, iFlyChatMessage> entry : threadHistoryMessageMap.entrySet()) {

                                if(threadHistoryCount==1)
                                    threadHistoryArrayCount=i;
                                i++;

                                iFlyChatMessage message = entry.getValue();
                                messageReceived(message);

                            }

                        }
                        else {
                            emptyUserThreadHistoryFlag=1;
                        }

                    }
                    threadHistoryCount=1;

                }
                else if (intent.getAction().equals("iFlyChat.onRoomThreadHistory")){

                    String roomId = intent.getStringExtra("forRoomId");

                    if(!ROOMID.equals("") && ROOMID.equals(roomId)){



                        LinkedHashMap<String, iFlyChatMessage> threadHistoryMessageMap =
                                (LinkedHashMap<String, iFlyChatMessage>) intent.getSerializableExtra("threadHistoryMap");

                        if(threadHistoryMessageMap!=null && threadHistoryMessageMap.size()>0){
                            int i=0;
                            for (Map.Entry<String, iFlyChatMessage> entry : threadHistoryMessageMap.entrySet()) {

                                if(threadHistoryCount==1)
                                    threadHistoryArrayCount=i;
                                i++;

                                iFlyChatMessage message = entry.getValue();
                                messageReceived(message);

                            }

                        }

                        else{

                            emptyRoomThreadHistoryFlag = 1;

                        }

                    }

                    threadHistoryCount=1;
                }
                else if(intent.getAction().equals("iFlyChat.onUploadProgress")){
                    HashMap<String, String> uploadProgressMap = (HashMap<String, String> )intent.getSerializableExtra("onUploadProgress");
                    String uploadMessageId = uploadProgressMap.get("messageId");
                    String progress = uploadProgressMap.get("progress");
                    int num = (int)Float.parseFloat(progress);

                    progressBar.setScaleY(3f);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(num);
                    if(num == 100){
                        progressBar.setVisibility(View.GONE);

                    }
                }
                else if(intent.getAction().equals("iFlyChat.onChatDisconnect"))
                {
                    top_bar.setBackgroundColor(Color.RED);
                    chat_status.setBackgroundColor(Color.RED);
                    chat_status.setText("Not connected");
                }
            }
        };

        sendMessage.setOnClickListener(onClickListener);

        sendAttachment.setOnClickListener(onClickListener);

        take_photo_layout.setOnClickListener(onClickListener);
        take_video_layout.setOnClickListener(onClickListener);
        select_file_layout.setOnClickListener(onClickListener);
        record_sound_layout.setOnClickListener(onClickListener);


    }


    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.send_message){
                attachmentOrTextCount=0;
                sendMessage(messageText.getText().toString());

                messageText.setText("");
            }
            else if (v.getId() == R.id.attachment) {
                if(sendAttachmentView.getVisibility() != View.VISIBLE){
                    sendAttachmentView.setVisibility(View.VISIBLE);
                    sendAttachmentView.bringToFront();
                }
                else if(sendAttachmentView.getVisibility() == View.VISIBLE){
                    sendAttachmentView.setVisibility(View.GONE);
                }

            }

            else if(v.getId() == R.id.take_photo_layout){
                attachmentOrTextCount=1;
                sendAttachmentView.setVisibility(View.GONE);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                // start the image capture Intent
                startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE);
            }

            else if(v.getId() == R.id.take_video_layout){
                attachmentOrTextCount=1;
                sendAttachmentView.setVisibility(View.GONE);
                Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

                intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                // start the video capture Intent
                startActivityForResult(intent, TAKE_VIDEO__REQUEST_CODE);
            }

            else if(v.getId() == R.id.select_file_layout){
                attachmentOrTextCount=1;
                sendAttachmentView.setVisibility(View.GONE);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, SELECT_FILE_REQUEST_CODE);
                }
            }
            else if(v.getId() == R.id.record_sound_layout){
                attachmentOrTextCount=1;
                sendAttachmentView.setVisibility(View.GONE);
                Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                startActivityForResult(intent, RECORD_SOUND_REQUEST_CODE);
            }



        }
    };



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == RECORD_SOUND_REQUEST_CODE){
            if (resultCode == RESULT_OK) {
                // Sound captured and saved to fileUri specified in the Intent
                fileUri = data.getData();
                sendMessage(fileUri.toString());
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the sound capture
            } else {
                // sound capture failed, advise user
            }
        }

        else if (requestCode == TAKE_PHOTO_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Photo captured and saved to fileUri specified in the Intent

                sendMessage(fileUri.toString());
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the photo capture
            } else {
                // photo capture failed, advise user
            }
        }

        else if (requestCode == TAKE_VIDEO__REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // video captured and saved to fileUri specified in the Intent

                sendMessage(fileUri.toString());

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
            } else {
                // video capture failed, advise user
            }
        }

        else if (requestCode == SELECT_FILE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                fileUri = data.getData();

                sendMessage(fileUri.toString());


            } else if (resultCode == RESULT_CANCELED) {

                // user cancelled file capture


            } else {
                // failed to capture file

            }

        }



    }


    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
              //  Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * Method to scroll the list view to bottom, when sender send a message and his/her message list view is in the middle.
     */
    private void scrollMyListViewToBottom() {
        messageListView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                if(messageListView.getCount()!=0)
                    messageListView.smoothScrollToPosition(messageListView.getCount() - 1);
            }
        });
    }

    /**
     * Method to update the UI, when message is received from an user.
     * @param message iFlyChatMessage object
     */
    public void messageReceived(iFlyChatMessage message)
    {
        if(messageAdapter == null)
        {
            if(messageList == null)
            {
                messageList = new ArrayList<iFlyChatMessage>();
            }
            messageList.add(message);
            currentMessageMap.put(message.getMessageId(), message);
            bindData();
        }
        else
        {
            if(threadHistoryArrayCount>-1){

                messageList.add(0 + threadHistoryArrayCount, message);

            } else
                messageList.add(message);


            currentMessageMap.put(message.getMessageId(), message);
            messageAdapter.notifyDataSetChanged();
            messageListView.requestFocusFromTouch();
            messageListView.setSelection(index + threadHistoryArrayCount);
            threadHistoryArrayCount =-1;
            // restore the position of listview


        }
    }

    /**
     * Method to update the UI, when message is send from an user to another user.
     * This method directly update the UI of sender instead of getting this send message from server and then displaying it in UI.
     * @param message iFlyChatMessage object
     */
    public void messageSend(iFlyChatMessage message)
    {
        if(messageAdapter == null)
        {
            if(messageList == null)
            {
                messageList = new ArrayList<iFlyChatMessage>();

            }
            messageList.add(message);
            currentMessageMap.put(message.getMessageId(), message);
            bindData();
        }
        else
        {
            messageList.add(message);
            currentMessageMap.put(message.getMessageId(), message);
            messageAdapter.notifyDataSetChanged();
        }
    }

    public void bindData()
    {
        messageAdapter = new MessageAdapter(messageList, getApplicationContext(), chatSettings);

        messageListView.setAdapter(messageAdapter);
    }

    // For sending message to user or room depending on the ID

    public void sendMessage(String messageString)
    {
        String toID = "";
        String toName = "";
        String type = "";
        if(!messageString.isEmpty())
        {
            if(!ROOMID.equals(""))
            {
                toID = ROOMID;
                toName = TONAME_ROOM;
                type= "room";

                iFlyChatMessage sendMessage = new iFlyChatMessage("",loggedUser.getId(),toID,USERNAME, toName,
                        messageString.trim(),"",loggedUser.getProfileUrl(),loggedUser.getAvatarUrl(),
                        loggedUser.getRole(),"",type);

                if(attachmentOrTextCount==0){
                    service.sendMessageToRoom(sendMessage);
                    messageSend(sendMessage);
                }
                else
                service.sendFileToRoom(sendMessage);


                if(roomCount==1)
                    scrollMyListViewToBottom();
                roomCount=1;

            }
            else if(!USERID.equals(""))
            {
                toID = USERID;
                toName = TONAME_USER+ " " +toID;
                type= "user";
                iFlyChatMessage sendMessage = new iFlyChatMessage("",loggedUser.getId(),toID,USERNAME,toName,
                        messageString.trim(),"",loggedUser.getProfileUrl(),loggedUser.getAvatarUrl(),
                        loggedUser.getRole(),"",type);

                if(attachmentOrTextCount==0) {
                    service.sendMessageToUser(sendMessage);
                    messageSend(sendMessage);
                }
                else {
                    service.sendFileToUser(sendMessage);
                }



                if(userCount==1)
                    scrollMyListViewToBottom();
                userCount=1;
            }
        }
    }



    // set gmail type circular image on top right corner

    private void setChatImage(Context context, ImageView userImage, ImageView userImageCircular, String id, String userName, String avatarUrl){

        Random ran = new Random();
        int number = ran.nextInt(2);

        String upToNCharacters = id.substring(0, Math.min(id.length(), 2));
        //User without Guest Prefix
        if(!upToNCharacters.equals("0-")){

            if(!avatarUrl.equals(null) && !avatarUrl.equals("")) {

                if (avatarUrl.contains("default_avatar") || avatarUrl.contains("gravatar")) {

                    if (defaultUserImageFlag == true) {

                        userImageCircular.setVisibility(View.VISIBLE);
                        userImage.setVisibility(View.GONE);
                        Drawable placeholder;

                        if (number == 0)
                            placeholder = ContextCompat.getDrawable(context, R.drawable.male_user);
                        else
                            placeholder = ContextCompat.getDrawable(context, R.drawable.female_user);

                        userImageCircular.setImageDrawable(placeholder);
                        //  userImageCircular.setBorderColor(mColorGenerator.getColor(userName));

                    } else {
                        char firstLetter = userName.charAt(0);
                        String name = Character.toString(firstLetter);
                        if(name.matches("[a-zA-Z]+")) {

                            userImageCircular.setVisibility(View.GONE);
                            userImage.setVisibility(View.VISIBLE);
                            char upperCaseLetter = Character.toUpperCase(userName.charAt(0));
                            TextDrawable drawable = mDrawableBuilder.build(String.valueOf(upperCaseLetter), mColorGenerator.getColor(userName));
                            userImage.setImageDrawable(drawable);
                            // holder.view.setBackgroundColor(Color.TRANSPARENT);
                        }
                        //Name Contains number or special character
                        else {

                            userImageCircular.setVisibility(View.VISIBLE);
                            userImage.setVisibility(View.GONE);
                            Drawable placeholder;

                            if(number==0)
                                placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                            else
                                placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                            userImageCircular.setImageDrawable(placeholder);
                            //  userImageCircular.setBorderColor(mColorGenerator.getColor(userName));

                        }

                    }
                } else {

                    if (userImageCircular != null) {
                        userImage.setVisibility(View.GONE);
                        userImageCircular.setVisibility(View.VISIBLE);

                        // check for url.
                        if (cancelPotentialDownload(avatarUrl, userImageCircular)) {
                            GetUsersTask task = new GetUsersTask(userImageCircular);
                            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                            // CircularImageView circularImageView = (CircularImageView) findViewById(R.id.yourCircularImageView);
                            userImageCircular.setImageDrawable(downloadedDrawable);
                            task.execute("http:" + avatarUrl);

                        }
                    }
                }
            }
            else if(defaultUserImageFlag==true) {

                userImageCircular.setVisibility(View.VISIBLE);
                userImage.setVisibility(View.GONE);
                Drawable placeholder;

                if(number==0)
                    placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                else
                    placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                userImageCircular.setImageDrawable(placeholder);
                //  userImageCircular.setBorderColor(mColorGenerator.getColor(userName));
            }
            else{
                char firstLetter = userName.charAt(0);
                String name = Character.toString(firstLetter);
                if(name.matches("[a-zA-Z]+")) {

                    userImageCircular.setVisibility(View.GONE);
                    userImage.setVisibility(View.VISIBLE);
                    char upperCaseLetter = Character.toUpperCase(userName.charAt(0));
                    TextDrawable drawable = mDrawableBuilder.build(String.valueOf(upperCaseLetter), mColorGenerator.getColor(userName));
                    userImage.setImageDrawable(drawable);
                    //  holder.view.setBackgroundColor(Color.TRANSPARENT);
                }
                //Name Contains number or special character
                else {

                    userImageCircular.setVisibility(View.VISIBLE);
                    userImage.setVisibility(View.GONE);
                    Drawable placeholder;

                    if(number==0)
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                    else
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                    userImageCircular.setImageDrawable(placeholder);
                    //   userImageCircular.setBorderColor(mColorGenerator.getColor(userName));


                }
            }

        }
        else{
            if(defaultUserImageFlag==true){

                if (userImageCircular != null) {
                    userImage.setVisibility(View.GONE);
                    userImageCircular.setVisibility(View.VISIBLE);

                    // check for url.
                    if (cancelPotentialDownload(avatarUrl, userImageCircular)) {
                        GetUsersTask task = new GetUsersTask(userImageCircular);
                        DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                        // CircularImageView circularImageView = (CircularImageView) findViewById(R.id.yourCircularImageView);
                        userImageCircular.setImageDrawable(downloadedDrawable);
                        task.execute("http:" + avatarUrl);

                    }
                }
            }
            else{

                String value  = chatSettings.get("guestPrefix");
                String name = userName.replaceFirst(value,"");

                if(name.matches("[a-zA-Z]+")) {
                    userImageCircular.setVisibility(View.GONE);
                    userImage.setVisibility(View.VISIBLE);
                    char upperCaseLetter = Character.toUpperCase(name.charAt(0));

                    TextDrawable drawable = mDrawableBuilder.build(String.valueOf(upperCaseLetter), mColorGenerator.getColor(name));
                    userImage.setImageDrawable(drawable);
                    //    holder.view.setBackgroundColor(Color.TRANSPARENT);
                }
                else{

                    userImageCircular.setVisibility(View.VISIBLE);
                    userImage.setVisibility(View.GONE);
                    Drawable placeholder;

                    if(number==0)
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                    else
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                    userImageCircular.setImageDrawable(placeholder);
                    //    userImageCircular.setBorderColor(mColorGenerator.getColor(userName));

                }
            }
        }

    }



    //AsyncTask to download the image from url asynchronously
    private class GetUsersTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> rowImageView;
        private String url;

        public GetUsersTask(ImageView imageView) {

            rowImageView = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap map = null;
            for (String url : urls) {
                this.url = url;
                map = downloadImage(url);
            }
            return map;
        }

        // Sets the Bitmap returned by doInBackground
        @Override
        protected void onPostExecute(Bitmap result) {
            if (isCancelled()) {
                result = null;
            }
            if (rowImageView != null) {
                ImageView imageView = rowImageView.get();
                GetUsersTask getUsersTask = getGetUsersTask(imageView);
                // Change bitmap only if this process is still associated with it
                if (this == getUsersTask) {
                    if (imageView != null) {
                        if (result != null) {
                            Drawable drawable = new BitmapDrawable(result);
                            imageView.setImageDrawable(drawable);
                        }
                    }

                }

            }

        }

        // Creates Bitmap from InputStream and returns it
        private Bitmap downloadImage(String url) {
            Bitmap bitmap = null;
            InputStream stream = null;
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inSampleSize = 1;

            try {
                stream = getHttpConnection(url);
                bitmap = BitmapFactory.
                        decodeStream(stream, null, bmOptions);
//                stream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return bitmap;
        }

        // Makes HttpURLConnection and returns InputStream
        private InputStream getHttpConnection(String urlString)
                throws IOException {
            InputStream stream = null;
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            try {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.setRequestMethod("GET");
                httpConnection.connect();

                if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    stream = httpConnection.getInputStream();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return stream;
        }
    }

    // Create a week reference of the async task.
    static class DownloadedDrawable extends BitmapDrawable {
        private final WeakReference<GetUsersTask> getUsersTaskReference;


        public DownloadedDrawable(GetUsersTask getUsersTask) {

            getUsersTaskReference =
                    new WeakReference<GetUsersTask>(getUsersTask);
        }

        public GetUsersTask getGetUsersTask() {
            return getUsersTaskReference.get();
        }


    }

    // Check for validity of the URL.
    private static boolean cancelPotentialDownload(String url, ImageView imageView) {
        GetUsersTask getUsersTask = getGetUsersTask(imageView);

        if (getUsersTask != null) {
            String bitmapUrl = getUsersTask.url;
            if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
                getUsersTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    private static GetUsersTask getGetUsersTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
                return downloadedDrawable.getGetUsersTask();
            }
        }
        return null;
    }


    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("iFlyChat.onChatConnect");
        intentFilter.addAction("iFlyChat.onChatDisconnect");
        intentFilter.addAction("iFlyChat.onMessageFromUser");
        intentFilter.addAction("iFlyChat.onMessageFromRoom");
        intentFilter.addAction("iFlyChat.onGlobalListUpdate");
        intentFilter.addAction("iFlyChat.onUserThreadHistory");
        intentFilter.addAction("iFlyChat.onRoomThreadHistory");
        intentFilter.addAction("iFlyChat.onUploadProgress");
        bManager.registerReceiver(receiver, intentFilter);

    }

    @Override
    protected  void onResume(){
        super.onResume();
    }

    @Override
    protected void  onPause(){
        super.onPause();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(service!=null)
        service.disconnectChat();
        super.onDestroy();
    }
}
