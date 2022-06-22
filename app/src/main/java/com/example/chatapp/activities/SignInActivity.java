package com.example.chatapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.chatapp.databinding.ActivitySignInBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity
{
    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
    
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS},
                PackageManager.PERMISSION_GRANTED);
        
        if ( preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN) )
        {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
        setListeners();
    }
    
    private void setListeners ()
    {
        binding.invokeSignUp.setOnClickListener(v -> startActivity(new Intent(SignInActivity.this, SignUpActivity.class)));
        
        binding.buttonSignIn.setOnClickListener(v ->
        {
            if ( isValidSignInCredentials() )
                signIn();
        });
    }
    
    private void showToast (String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void signIn ()
    {
        progress(true);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE, binding.inputPhone.getText().toString().trim())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString().trim())
                .get()
                .addOnCompleteListener(task ->
                {
                    if ( task.isSuccessful() && task.getResult() != null
                            && task.getResult().getDocuments().size() > 0 )
                    {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    else
                    {
                        showToast("Unable to Sign In");
                        progress(false);
                    }
                });
    }
    
    private void progress (Boolean isProgressing)
    {
        if ( isProgressing )
        {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
        }
        else
        {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }
    
    private Boolean isValidSignInCredentials ()
    {
        if ( binding.inputPhone.getText().toString().trim().isEmpty() )
        {
            showToast("Enter Email-Id");
            return false;
        }
        else if ( !Patterns.PHONE.matcher(binding.inputPhone.getText().toString().trim()).matches() )
        {
            showToast("Enter valid phone number");
            return false;
        }
        else if ( binding.inputPassword.getText().toString().trim().isEmpty() )
        {
            showToast("Enter Password");
            return false;
        }
        else
            return true;
    }
    
}