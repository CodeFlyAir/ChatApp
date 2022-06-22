package com.example.chatapp.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.chatapp.R;
import com.example.chatapp.adapters.UsersAdapters;
import com.example.chatapp.databinding.ActivityUsersBinding;
import com.example.chatapp.listeners.UserListener;
import com.example.chatapp.models.Users;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UsersActivity extends BaseActivity implements UserListener
{
    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        
        setListeners();
        getUsers();
        
    }
    
    private void setListeners ()
    {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }
    
    private void getUsers ()
    {
        progress(true);
        try
        {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection(Constants.KEY_COLLECTION_USERS)
                    .get()
                    .addOnCompleteListener(task ->
                    {
                        progress(false);
                        String currentUserID = preferenceManager.getString(Constants.KEY_USER_ID);
                        if ( task.isSuccessful() && task.getResult() != null )
                        {
                            List<Users> users = new ArrayList<>();
                            for ( QueryDocumentSnapshot queryDocumentSnapshot : task.getResult() )
                            {
                                if ( currentUserID.equals(queryDocumentSnapshot.getId()) )
                                    continue;
                                
                                Users user = new Users();
                                user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                //user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                user.phone = queryDocumentSnapshot.getString(Constants.KEY_PHONE);
                                
                                Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(user.phone));
                                Cursor cursor = getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}
                                        , null, null, null);
                                
                                if ( cursor != null )
                                {
                                    if ( cursor.moveToFirst() )
                                    {
                                        user.name = cursor.getString(0);
                                        user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                        user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                        user.id = queryDocumentSnapshot.getId();
                                        users.add(user);
                                    }
                                }
                                
                            }
                            if ( users.size() > 0 )
                            {
                                UsersAdapters usersAdapters = new UsersAdapters(users, this);
                                binding.usersRecyclerView.setAdapter(usersAdapters);
                                binding.usersRecyclerView.setVisibility(View.VISIBLE);
                            }
                            else
                                showErrorMessage();
                        }
                        else
                            showErrorMessage();
                    });
        } catch ( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    private void showErrorMessage ()
    {
        binding.textErrorMessage.setText(String.format("%s", "No User Available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }
    
    private void progress (Boolean isProgressing)
    {
        if ( isProgressing )
            binding.progressBar.setVisibility(View.VISIBLE);
        else
            binding.progressBar.setVisibility(View.INVISIBLE);
    }
    
    @Override
    public void onUserClicked (Users users)
    {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, users);
        startActivity(intent);
        finish();
    }
}