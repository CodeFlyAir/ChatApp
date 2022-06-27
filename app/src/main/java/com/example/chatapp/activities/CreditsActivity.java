package com.example.chatapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.databinding.ActivityCreditsBinding;

public class CreditsActivity extends AppCompatActivity
{
    private ActivityCreditsBinding binding;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityCreditsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setListeners();
    }
    
    private void setListeners ()
    {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        
        binding.github.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/CodeFlyAir"))));
        
        binding.linkedin.setOnClickListener(v ->
                
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/rungshit-saha"))));
        
        
        binding.instagram.setOnClickListener(v ->
                
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/in_the_sierra_kilo_yankee"))));
        
    }
    
}