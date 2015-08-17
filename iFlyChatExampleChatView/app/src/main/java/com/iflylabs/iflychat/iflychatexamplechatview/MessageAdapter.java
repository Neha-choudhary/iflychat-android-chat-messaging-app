package com.iflylabs.iflychat.iflychatexamplechatview;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;


import com.iflylabs.iFlyChatLibrary.iFlyChatMessage;

import java.text.ParseException;
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

        Long epoch = Long.parseLong(message.getTime())*1000;


        String currentTime = sdf.format(new Date(epoch));

        holder.message_time.setText(currentTime);

        holder.message_row_image.setImageResource(R.drawable.default_avatar);

        return convertView;
    }


    static class ViewHolder {
        TextView userName;
        TextView message_text;
        TextView message_time;
        ImageView message_row_image;
    }
}