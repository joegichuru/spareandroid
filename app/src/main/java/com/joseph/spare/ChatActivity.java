package com.joseph.spare;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.joseph.spare.domain.Author;
import com.joseph.spare.domain.ChatMessage;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ChatActivity extends AppCompatActivity {

    @BindView(R.id.message_list)
    MessagesList messagesList;
    MessagesListAdapter<ChatMessage> messagesListAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);
        messagesListAdapter=new MessagesListAdapter<>("1",null);
        messagesList.setAdapter(messagesListAdapter);
        mockUsers();
    }

    public void mockUsers(){
        Author sender=new Author();
        sender.setAvatar("dgd");
        sender.setId("1");
        sender.setName("Joseph");
        Author recep=new Author();
        recep.setAvatar("dgd");
        recep.setId("2");
        recep.setName("Sandra");

        List<ChatMessage> chatMessages=new ArrayList<>();
        for(int i=0;i<10;i++){
            ChatMessage chatMessage=new ChatMessage();
            chatMessage.setId(String.valueOf(i));
            chatMessage.setTimeStamp(new Date().getTime());
            chatMessage.setText("Messagsddvdgdg dgddgdgdgdg dgdgdgdgdg dgddggdgdgd ddgd  gwdjdj "+i);
            if(i%2==0){
                chatMessage.setUser(sender);
            }else {
                chatMessage.setUser(recep);
            }
            chatMessages.add(chatMessage);
        }
        messagesListAdapter.addToEnd(chatMessages,true);

    }

}
