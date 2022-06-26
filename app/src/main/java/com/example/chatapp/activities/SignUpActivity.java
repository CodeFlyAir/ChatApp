package com.example.chatapp.activities;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.chaos.view.PinView;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SignUpActivity extends AppCompatActivity
{
    private ActivitySignUpBinding binding;
    private PinView pinView;
    private Button verifyButton;
    private String encodedImage;
    private PreferenceManager preferenceManager;
    private FirebaseAuth mAuth;
    private Dialog dialog;
    private String verificationId;
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mAuth = FirebaseAuth.getInstance();
        
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.item_otp_dialog);
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.background_otp);
        dialog.create();
        
        pinView = dialog.findViewById(R.id.pinViewText);
        verifyButton = dialog.findViewById(R.id.verifyButton);
        
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
    }
    
    private void setListeners ()
    {
        binding.invokeSignIn.setOnClickListener(v -> startActivity(new Intent(SignUpActivity.this, SignInActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)));
        
        binding.buttonSignUp.setOnClickListener(v ->
        {
            checkForExistingUser(binding.inputPhone.getText().toString().trim());
            if ( isValidCredentials() && !isFinishing() )
            {
                verifyPhoneNumber();
            }
        });
        
        binding.imageLayout.setOnClickListener(v ->
        {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
        
        verifyButton.setOnClickListener(v ->
        {
            if ( pinView.getText() == null || pinView.getText().toString().length() < 6 )
                Toast.makeText(SignUpActivity.this, "Enter Valid OTP", Toast.LENGTH_SHORT).show();
            else
            {
                verifyCode(pinView.getText().toString());
            }
        });
        
        pinView.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged (CharSequence s, int start, int count, int after)
            {
            
            }
            
            @Override
            public void onTextChanged (CharSequence s, int start, int before, int count)
            {
                if ( s.toString().length() == 6 )
                {
                    verifyCode(Objects.requireNonNull(pinView.getText()).toString());
                }
            }
            
            @Override
            public void afterTextChanged (Editable s)
            {
            
            }
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
        map.put(Constants.KEY_PHONE, binding.inputPhone.getText().toString().trim());
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
    
    private void verifyPhoneNumber ()
    {
        String phoneNumber = "+91" + binding.inputPhone.getText().toString().trim();
        openDialog();
        sendVerificationCode(phoneNumber);
    }
    
    private void openDialog ()
    {
        dialog.show();
    }
    
    private void sendVerificationCode (String phoneNumber)
    {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallBack)
                        .build();
        
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
    
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks
            mCallBack = new PhoneAuthProvider.OnVerificationStateChangedCallbacks()
    {
        @Override
        public void onCodeSent (@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken)
        {
            super.onCodeSent(s, forceResendingToken);
            verificationId = s;
            showToast("OTP Sent");
        }
        
        @Override
        public void onVerificationCompleted (@NonNull PhoneAuthCredential phoneAuthCredential)
        {
        
        }
        
        @Override
        public void onVerificationFailed (@NonNull FirebaseException e)
        {
            showToast("Verification Failed");
            Log.d("OTP", e.getMessage());
        }
    };
    
    protected void verifyCode (String smsCode)
    {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, smsCode);
        authenticateUser(credential);
    }
    
    private void authenticateUser (PhoneAuthCredential credential)
    {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task ->
                {
                    if ( task.isSuccessful() )
                    {
                        signUp();
                    }
                    else
                    {
                        Toast.makeText(SignUpActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
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
        checkForExistingUser(binding.inputPhone.getText().toString().trim());
        if ( encodedImage == null )
        {
            Bitmap bitmap=BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.blankprofilepicture);
            encodedImage=encodeImage(bitmap);
            binding.imageProfile.setImageBitmap(bitmap);
            return true;
        }
        else if ( binding.inputName.getText().toString().trim().isEmpty() )
        {
            showToast("Enter Name");
            return false;
        }
        else if ( binding.inputPhone.getText().toString().trim().isEmpty() )
        {
            showToast("Enter Phone Number");
            return false;
        }
        else if ( !Patterns.PHONE.matcher(binding.inputPhone.getText().toString().trim()).matches()
                || binding.inputPhone.getText().toString().trim().length() != 10 )
        {
            showToast("Enter valid Phone Number");
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
        else if ( binding.inputPassword.getText().toString().trim().length()<6 )
        {
            showToast("Minimum Password length is 6");
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
    
    private void checkForExistingUser (String phone)
    {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>()
                {
                    @Override
                    public void onComplete (@NonNull Task<QuerySnapshot> task)
                    {
                        if ( task.isSuccessful() && task.getResult() != null )
                        {
                            for ( QueryDocumentSnapshot queryDocumentSnapshot : task.getResult() )
                            {
                                if ( phone.equals(queryDocumentSnapshot.getString(Constants.KEY_PHONE)) )
                                {
                                    showToast("Account already exists");
                                    finish();
                                }
                            }
                        }
                    }
                });
        
    }
}