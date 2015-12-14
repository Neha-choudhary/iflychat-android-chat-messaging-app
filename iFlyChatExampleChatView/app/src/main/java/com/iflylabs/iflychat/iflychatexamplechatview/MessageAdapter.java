package com.iflylabs.iflychat.iflychatexamplechatview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.iflylabs.iFlyChatLibrary.iFlyChatMessage;
import com.iflylabs.iflychatexamplechatview.R;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class MessageAdapter extends BaseAdapter {

    List<iFlyChatMessage> messages;
    Context context;
    SharedPreferences loginSession;

    private ColorGenerator mColorGenerator = ColorGenerator.MATERIAL;
    private TextDrawable.IBuilder mDrawableBuilder = TextDrawable.builder().round();
    private LayoutInflater layoutInflater;
    boolean defaultUserImageFlag = false;
    private HashMap<String ,String> chatSettings;

    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");


    public MessageAdapter(List<iFlyChatMessage> messageList, Context mContext, HashMap<String ,String> chatSettings) {
        messages = messageList;
        this.context = mContext;
        this.chatSettings=chatSettings;
        layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loginSession = mContext.getSharedPreferences("loginSession",0);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (messages.get(position).getFromId().equals(loginSession.getString("uid",null))) ? 0 : 1;
    }


    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View messageView, ViewGroup parent) {

        ViewHolder holder;

        int type = getItemViewType(position);


        if (messageView == null)
        {

            if(type == 0)
            {
                messageView = layoutInflater.inflate(R.layout.sender_message_row, parent, false);

            }
            else if(type == 1)
            {
                messageView = layoutInflater.inflate(R.layout.receiver_message_row, parent, false);
            }

            holder = new ViewHolder(messageView);
            messageView.setTag(holder);

        }
        else
        {
            holder = (ViewHolder) messageView.getTag();
        }

        iFlyChatMessage message = (iFlyChatMessage) messages.get(position);

        holder.userName.setText(message.getFromName());

        holder.message_text.setText(message.getMessage());

        //To find todays date
        Date date = new Date();
        Long time = date.getTime();
        String currentTimeAndDate = sdf.format(new Date(time));
        String currentDate = currentTimeAndDate.substring(0, 5);

        //Message Date and Time
        Long messageEpoch= 0l;

        if(message.getTime().length()==10){
            messageEpoch = Long.parseLong(message.getTime())*1000;
        }
        else
            messageEpoch = Long.parseLong(message.getTime());

        String currentMessageTimeAndDate = sdf.format(new Date(messageEpoch));

        String currentMessageDate = currentMessageTimeAndDate.substring(0, 5);
       // String currentMessageDate  = "03/09";
        String currentMessageTime = currentMessageTimeAndDate.substring(6, 11);

        // If todays date and message date is same then display only message time. Otherwise, display both date and time.
        if(currentDate.equals(currentMessageDate)){

            holder.message_time.setText(currentMessageTime);
        }
        else{
            holder.message_time.setText(currentMessageTimeAndDate);
        }

        // get FromAvatarUrl of the user and if it is null or empty show the default Url.
        String avatarUrl = messages.get(position).getFromAvatarUrl();

        setChatImage(holder, messages.get(position).getFromId(), messages.get(position).getFromName(), avatarUrl);
        return messageView;
    }


    static class ViewHolder {
        TextView userName;
        TextView message_text;
        TextView message_time;
        ImageView message_row_image;
        CircularImageView message_row_circular_image;


        private View view;
        private int number;


        private ViewHolder(View messageView) {

            this.view = messageView;
            userName = (TextView) messageView.findViewById(R.id.username);
            message_text = (TextView) messageView.findViewById(R.id.message_text);

            message_time = (TextView) messageView.findViewById(R.id.message_time);

            message_row_circular_image = (CircularImageView) messageView.findViewById(R.id.message_row_circular_image);
            message_row_image = (ImageView) messageView.findViewById(R.id.message_row_image);

            Random ran = new Random();
            int number = ran.nextInt(2);
            this.number = number;
        }
    }

    private void setChatImage(ViewHolder holder, String id, String userName, String avatarUrl){

        String upToNCharacters = id.substring(0, Math.min(id.length(), 2));
        //User without Guest Prefix
        if(!upToNCharacters.equals("0-")){

            if(!avatarUrl.equals(null) && !avatarUrl.equals("")) {

                if (avatarUrl.contains("default_avatar") || avatarUrl.contains("gravatar")) {

                    if (defaultUserImageFlag == true) {

                        holder.message_row_circular_image.setVisibility(View.VISIBLE);
                        holder.message_row_image.setVisibility(View.GONE);
                        Drawable placeholder;

                        if (holder.number == 0)
                            placeholder = ContextCompat.getDrawable(context, R.drawable.male_user);
                        else
                            placeholder = ContextCompat.getDrawable(context, R.drawable.female_user);

                        holder.message_row_circular_image.setImageDrawable(placeholder);
                        holder.message_row_circular_image.setBorderColor(mColorGenerator.getColor(userName));

                    } else {
                        char firstLetter = userName.charAt(0);
                        String name = Character.toString(firstLetter);
                        if(name.matches("[a-zA-Z]+")) {

                            holder.message_row_circular_image.setVisibility(View.GONE);
                            holder.message_row_image.setVisibility(View.VISIBLE);
                            char upperCaseLetter = Character.toUpperCase(userName.charAt(0));
                            TextDrawable drawable = mDrawableBuilder.build(String.valueOf(upperCaseLetter), mColorGenerator.getColor(userName));
                            holder.message_row_image.setImageDrawable(drawable);
                            holder.view.setBackgroundColor(Color.TRANSPARENT);
                        }
                        //Name Contains number or special character
                        else {

                            holder.message_row_circular_image.setVisibility(View.VISIBLE);
                            holder.message_row_image.setVisibility(View.GONE);
                            Drawable placeholder;

                            if(holder.number==0)
                                placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                            else
                                placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                            holder.message_row_circular_image.setImageDrawable(placeholder);
                            holder.message_row_circular_image.setBorderColor(mColorGenerator.getColor(userName));

                        }

                    }
                } else {

                    if (holder.message_row_circular_image != null) {
                        holder.message_row_image.setVisibility(View.GONE);
                        holder.message_row_circular_image.setVisibility(View.VISIBLE);

                        // check for url.
                        if (cancelPotentialDownload(avatarUrl, holder.message_row_circular_image)) {
                            GetUsersTask task = new GetUsersTask(holder.message_row_circular_image);
                            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                            // CircularImageView circularImageView = (CircularImageView) findViewById(R.id.yourCircularImageView);
                            holder.message_row_circular_image.setImageDrawable(downloadedDrawable);
                            task.execute("http:" + avatarUrl);

                        }
                    }
                }
            }
            else if(defaultUserImageFlag==true) {

                holder.message_row_circular_image.setVisibility(View.VISIBLE);
                holder.message_row_image.setVisibility(View.GONE);
                Drawable placeholder;

                if(holder.number==0)
                    placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                else
                    placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                holder.message_row_circular_image.setImageDrawable(placeholder);
                holder.message_row_circular_image.setBorderColor(mColorGenerator.getColor(userName));
            }
            else{
                char firstLetter = userName.charAt(0);
                String name = Character.toString(firstLetter);
                if(name.matches("[a-zA-Z]+")) {

                    holder.message_row_circular_image.setVisibility(View.GONE);
                    holder.message_row_image.setVisibility(View.VISIBLE);
                    char upperCaseLetter = Character.toUpperCase(userName.charAt(0));
                    TextDrawable drawable = mDrawableBuilder.build(String.valueOf(upperCaseLetter), mColorGenerator.getColor(userName));
                    holder.message_row_image.setImageDrawable(drawable);
                    holder.view.setBackgroundColor(Color.TRANSPARENT);
                }
                //Name Contains number or special character
                else {

                    holder.message_row_circular_image.setVisibility(View.VISIBLE);
                    holder.message_row_image.setVisibility(View.GONE);
                    Drawable placeholder;

                    if(holder.number==0)
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                    else
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                    holder.message_row_circular_image.setImageDrawable(placeholder);
                    holder.message_row_circular_image.setBorderColor(mColorGenerator.getColor(userName));


                }
            }

        }
        else{
            if(defaultUserImageFlag==true){

                if (holder.message_row_circular_image != null) {
                    holder.message_row_image.setVisibility(View.GONE);
                    holder.message_row_circular_image.setVisibility(View.VISIBLE);

                    // check for url.
                    if (cancelPotentialDownload(avatarUrl, holder.message_row_circular_image)) {
                        GetUsersTask task = new GetUsersTask(holder.message_row_circular_image);
                        DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                        // CircularImageView circularImageView = (CircularImageView) findViewById(R.id.yourCircularImageView);
                        holder.message_row_circular_image.setImageDrawable(downloadedDrawable);
                        task.execute("http:" + avatarUrl);

                    }
                }
            }
            else{

                String value  = chatSettings.get("guestPrefix");
                String name = userName.replaceFirst(value,"");

                if(name.matches("[a-zA-Z]+")) {

                    holder.message_row_circular_image.setVisibility(View.GONE);
                    holder.message_row_image.setVisibility(View.VISIBLE);
                    char upperCaseLetter = Character.toUpperCase(name.charAt(0));

                    TextDrawable drawable = mDrawableBuilder.build(String.valueOf(upperCaseLetter), mColorGenerator.getColor(name));
                    holder.message_row_image.setImageDrawable(drawable);
                    holder.view.setBackgroundColor(Color.TRANSPARENT);
                }
                else{

                    holder.message_row_circular_image.setVisibility(View.VISIBLE);
                    holder.message_row_image.setVisibility(View.GONE);
                    Drawable placeholder;

                    if(holder.number==0)
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.male_user);
                    else
                        placeholder =   ContextCompat.getDrawable(context, R.drawable.female_user);

                    holder.message_row_circular_image.setImageDrawable(placeholder);
                    holder.message_row_circular_image.setBorderColor(mColorGenerator.getColor(userName));

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

}