package com.iflylabs.iflychat.iflychatexamplechatview;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.iflylabs.iFlyChatLibrary.iFlyChatMessage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by prateek on 13/08/15.
 */
public class MessageAdapter extends BaseAdapter {

    List<iFlyChatMessage> messages;
    Context mContext;

    SharedPreferences loginSession;

    private LayoutInflater layoutInflater;

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");


    public MessageAdapter(List<iFlyChatMessage> messageList, Context mContext) {
        messages = messageList;
        this.mContext = mContext;
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
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        holder = new ViewHolder();
        NinePatchDrawable background;

        int type = getItemViewType(position);


        if (convertView == null)
        {

            if(type == 0)
            {
                convertView = layoutInflater.inflate(R.layout.message_row_self, parent, false);

            }
            else if(type == 1)
            {
                convertView = layoutInflater.inflate(R.layout.message_row_other, parent, false);
            }



            holder.userName = (TextView) convertView.findViewById(R.id.username);

            holder.message_text = (TextView) convertView.findViewById(R.id.message_text);

            holder.message_time = (TextView) convertView.findViewById(R.id.message_time);

            holder.message_row_image = (ImageView) convertView.findViewById(R.id.message_row_image);

            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolder) convertView.getTag();
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
        Long messageEpoch = Long.parseLong(message.getTime())*1000;

        String currentMessageTimeAndDate = sdf.format(new Date(messageEpoch));

        String currentMessageDate = currentMessageTimeAndDate.substring(0, 5);
       // String currentMessageDate  = "03/09";
        String currentMessageTime = currentMessageTimeAndDate.substring(6, 11);

        // If todays date and message date is same then display only message time. Otherwise, display both date and time.
        if(currentDate.equals(currentMessageDate))
        holder.message_time.setText(currentMessageTime);
        else
        holder.message_time.setText(currentMessageTimeAndDate);

        // get FromAvatarUrl of the user and if it is null or empty show the default Url.
        String avatarUrl = messages.get(position).getFromAvatarUrl();
        if (avatarUrl.equals(null) || avatarUrl.equals("")) {
            avatarUrl = "//cdn.iflychat.com/mobile/images/default_avatar.png";
        }

        // Create an object for subclass of AsyncTask
        if (holder.message_row_image != null) {
            // check for url.
            if (cancelPotentialDownload(avatarUrl, holder.message_row_image)) {
                GetUsersTask task = new GetUsersTask(holder.message_row_image);
                DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
                holder.message_row_image.setImageDrawable(downloadedDrawable);
                task.execute("http:" + avatarUrl);
            }
        }

        return convertView;
    }


    static class ViewHolder {
        TextView userName;
        TextView message_text;
        TextView message_time;
        ImageView message_row_image;
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
                        } else {
                            Drawable placeholder = imageView.getContext().getResources().getDrawable(R.drawable.default_avatar);
                            imageView.setImageDrawable(placeholder);
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
                stream.close();
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