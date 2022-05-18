package com.example.chatapp.adapters;

import android.graphics.Bitmap;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.ItemContainerRecievedMessageBinding;
import com.example.chatapp.databinding.ItemContainerSentMessageBinding;
import com.example.chatapp.models.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private final Bitmap receiverProfileImage;
    private final List<ChatMessage> chatMessages;
    private final String senderId;
    
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    
    public ChatAdapter (Bitmap receiverProfileImage, List<ChatMessage> chatMessages, String senderId)
    {
        this.receiverProfileImage = receiverProfileImage;
        this.chatMessages = chatMessages;
        this.senderId = senderId;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType)
    {
        return null;
    }
    
    @Override
    public void onBindViewHolder (@NonNull RecyclerView.ViewHolder holder, int position)
    {
    
    }
    
    @Override
    public int getItemCount ()
    {
        return 0;
    }
    
    static class SentMessageViewHolder extends RecyclerView.ViewHolder
    {
        private final ItemContainerSentMessageBinding binding;
        
        SentMessageViewHolder (ItemContainerSentMessageBinding itemContainerSentMessageBinding)
        {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }
        
        void setData (ChatMessage chatMessage)
        {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateAndTime.setText(chatMessage.dateTime);
        }
    }
    
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder
    {
        private final ItemContainerRecievedMessageBinding binding;
        
        ReceivedMessageViewHolder (ItemContainerRecievedMessageBinding itemContainerRecievedMessageBinding)
        {
            super(itemContainerRecievedMessageBinding.getRoot());
            binding = itemContainerRecievedMessageBinding;
        }
        
        void setData (ChatMessage chatMessage, Bitmap receiverProfileImage)
        {
            binding.textMessage.setText(chatMessage.message);
            binding.textDateAndTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(receiverProfileImage);
        }
    }
}
