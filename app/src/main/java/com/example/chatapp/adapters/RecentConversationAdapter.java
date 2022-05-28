package com.example.chatapp.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.databinding.ItemContainerRecentConversationBinding;
import com.example.chatapp.models.ChatMessage;

import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversationViewHolder>
{
    private final List<ChatMessage> chatMessages;
    
    public RecentConversationAdapter (List<ChatMessage> chatMessages)
    {
        this.chatMessages = chatMessages;
    }
    
    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder (@NonNull ViewGroup parent, int viewType)
    {
        return new ConversationViewHolder(
                ItemContainerRecentConversationBinding.inflate(
                        LayoutInflater.from(parent.getContext())
                        , parent
                        , false)
        );
    }
    
    @Override
    public void onBindViewHolder (@NonNull ConversationViewHolder holder, int position)
    {
        holder.setData(chatMessages.get(position));
    }
    
    @Override
    public int getItemCount ()
    {
        return chatMessages.size();
    }
    
    class ConversationViewHolder extends RecyclerView.ViewHolder
    {
        ItemContainerRecentConversationBinding binding;
        
        ConversationViewHolder (ItemContainerRecentConversationBinding itemContainerRecentConversationBinding)
        {
            super(itemContainerRecentConversationBinding.getRoot());
            binding = itemContainerRecentConversationBinding;
        }
        
        void setData (ChatMessage chatMessage)
        {
            binding.imageProfile.setImageBitmap(getConversationImage(chatMessage.conversationImage));
            binding.textName.setText(chatMessage.conversationName);
            binding.textRecentMessages.setText(chatMessage.message);
        }
        
    }
    
    private Bitmap getConversationImage (String encodedImage)
    {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
