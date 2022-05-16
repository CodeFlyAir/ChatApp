package com.example.chatapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity
{
    private ActivityChatBinding binding;
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}