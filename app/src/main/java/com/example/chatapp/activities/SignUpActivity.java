package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity
{
    private ActivitySignUpBinding binding;
    private String encodedImage;
    private PreferenceManager preferenceManager;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }
    
    private void setListeners ()
    {
        binding.invokeSignIn.setOnClickListener(v -> startActivity(new Intent(SignUpActivity.this, SignInActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        
        binding.buttonSignUp.setOnClickListener(v -> signUp());
        
        binding.imageLayout.setOnClickListener(v ->
        {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }
    
    private void showToast (String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void signUp ()
    {
        progress(true);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        HashMap<String, Object> map = new HashMap<>();
        map.put(Constants.KEY_NAME, binding.inputName.getText().toString().trim());
        map.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString().trim());
        map.put(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString().trim());
        map.put(Constants.KEY_IMAGE, encodedImage);
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .add(map)
                .addOnSuccessListener(documentReference ->
                {
                    progress(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString().trim());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                {
                    progress(false);
                    showToast(e.getMessage());
                    Log.d("FCM", e.getMessage());
                });
    }
    
    private String encodeImage (Bitmap bitmap)
    {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>()
            {
                @Override
                public void onActivityResult (ActivityResult result)
                {
                    if ( result.getResultCode() == RESULT_OK )
                    {
                        if ( result.getData() != null )
                        {
                            Uri imageUri = result.getData().getData();
                            try
                            {
                                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                binding.imageProfile.setImageBitmap(bitmap);
                                binding.textAddImage.setVisibility(View.GONE);
                                encodedImage = encodeImage(bitmap);
                                
                            } catch ( Exception e )
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
    );
    
    private void progress (Boolean isProgressing)
    {
        if ( isProgressing )
        {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
        }
        else
        {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }
    
    private Boolean isValidCredentials ()
    {
        if ( encodedImage == null )
        {
            showToast("Select Profile Image");
            return false;
        }
        else if ( binding.inputName.getText().toString().trim().isEmpty() )
        {
            showToast("Enter Name");
            return false;
        }
        else if ( binding.inputEmail.getText().toString().trim().isEmpty() )
        {
            showToast("Enter Email-Id");
            return false;
        }
        else if ( !Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString().trim()).matches() )
        {
            showToast("Enter valid email");
            return false;
        }
        else if ( binding.inputPassword.getText().toString().trim().isEmpty() )
        {
            showToast("Enter Password");
            return false;
        }
        else if ( binding.inputConfirmPassword.getText().toString().trim().isEmpty() )
        {
            showToast("Confirm Password");
            return false;
        }
        else if ( !binding.inputPassword.getText().toString().trim().equals(binding.inputConfirmPassword.getText().toString().trim()) )
        {
            showToast("Password and Confirm Password fields must match");
            return false;
        }
        else
            return true;
    }
}