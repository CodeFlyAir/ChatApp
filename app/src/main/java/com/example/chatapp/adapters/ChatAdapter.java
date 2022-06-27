package com.example.chatapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.ItemContainerRecievedMessageBinding;
import com.example.chatapp.databinding.ItemContainerSentMessageBinding;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.utilities.Constants;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
{
    private Bitmap receiverProfileImage;
    private final List<ChatMessage> chatMessages;
    private final String senderId;
    
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    
    public void setReceiverProfileImage (Bitmap bitmap)
    {
        receiverProfileImage = bitmap;
    }
    
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
        if ( viewType == VIEW_TYPE_SENT )
        {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(LayoutInflater.from(parent.getContext()),
                            parent, false));
        }
        else
        {
            return new ReceivedMessageViewHolder(
                    ItemContainerRecievedMessageBinding.inflate(LayoutInflater.from(parent.getContext()),
                            parent, false));
            
        }
    }
    
    @Override
    public void onBindViewHolder (@NonNull RecyclerView.ViewHolder holder, int position)
    {
        if ( getItemViewType(position) == VIEW_TYPE_SENT )
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        else
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
    }
    
    @Override
    public int getItemCount ()
    {
        return chatMessages.size();
    }
    
    @Override
    public int getItemViewType (int position)
    {
        if ( chatMessages.get(position).senderId.equals(senderId) )
            return VIEW_TYPE_SENT;
        else
            return VIEW_TYPE_RECEIVED;
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
            if ( Objects.equals(chatMessage.messageType, Constants.KEY_MESSAGE_TYPE_IS_TEXT) )
            {
                binding.textMessage.setText(chatMessage.message);
                binding.textDateAndTime.setText(chatMessage.dateTime);
            }
            else if ( Objects.equals(chatMessage.messageType, Constants.KEY_MESSAGE_TYPE_IS_IMAGE) )
            {
                binding.textMessage.setVisibility(View.GONE);
                binding.textDateAndTime.setVisibility(View.GONE);
                binding.imagePlaceholder.setVisibility(View.VISIBLE);
                
                Picasso.get().load(chatMessage.message).into(binding.imagePlaceholder);
            }
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
            if ( Objects.equals(chatMessage.messageType, Constants.KEY_MESSAGE_TYPE_IS_TEXT) )
            {
                binding.textMessage.setText(chatMessage.message);
                binding.textDateAndTime.setText(chatMessage.dateTime);
                
                if ( receiverProfileImage != null )
                {
                    binding.imageProfile.setImageBitmap(receiverProfileImage);
                }
            }
            else if ( Objects.equals(chatMessage.messageType, Constants.KEY_MESSAGE_TYPE_IS_IMAGE) )
            {
                binding.textMessage.setVisibility(View.GONE);
                binding.textDateAndTime.setVisibility(View.GONE);
                binding.imagePlaceholder.setVisibility(View.VISIBLE);
                
                Picasso.get().load(chatMessage.message).into(binding.imagePlaceholder);
            }
            
        }
    }
}
