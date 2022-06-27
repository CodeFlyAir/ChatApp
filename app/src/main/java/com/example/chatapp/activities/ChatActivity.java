package com.example.chatapp.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.example.chatapp.adapters.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.Users;
import com.example.chatapp.network.ApiClient;
import com.example.chatapp.network.ApiService;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity
{
    private ActivityChatBinding binding;
    private Users receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversationId = null;
    private boolean isReceiverAvailable = false;
    
    
    @Override
    protected void onCreate (Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessages();
    }
    
    private void sendMessage ()
    {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_IS_TEXT);
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        
        recentConversation(binding.inputMessage.getText().toString(), Constants.KEY_MESSAGE_TYPE_IS_TEXT);
        
        if ( !isReceiverAvailable )
        {
            try
            {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);
                
                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
                
                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);
                
                sendNotification(body.toString());
            } catch ( Exception e )
            {
                showToast(e.getMessage());
            }
        }
        
        binding.inputMessage.setText(null);
    }
    
    private void recentConversation (String lastMessage, String messageType)
    {
        if ( conversationId != null )
        {
            updateConversation(lastMessage, messageType);
        }
        else
        {
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversation.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversation.put(Constants.KEY_LAST_MESSAGE, lastMessage);
            conversation.put(Constants.KEY_MESSAGE_TYPE, messageType);
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            addConversation(conversation);
        }
    }
    
    private void listenAvailabilityOfReceiver ()
    {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(receiverUser.id)
                .addSnapshotListener(ChatActivity.this, (value, error) ->
                {
                    if ( error != null )
                        return;
                    if ( value != null )
                    {
                        if ( value.getLong(Constants.KEY_AVAILABILITY) != null )
                        {
                            int availability = Objects.requireNonNull(
                                    value.getLong(Constants.KEY_AVAILABILITY)
                            ).intValue();
                            isReceiverAvailable = availability == 1;
                        }
                        receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                        
                        if ( receiverUser.image == null )
                        {
                            receiverUser.image = value.getString(Constants.KEY_RECEIVER_IMAGE);
                            chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                            chatAdapter.notifyItemRangeChanged(0, chatMessages.size());
                        }
                    }
                    if ( isReceiverAvailable )
                        binding.textAvailability.setVisibility(View.VISIBLE);
                    else
                        binding.textAvailability.setVisibility(View.GONE);
                });
    }
    
    private void listenMessages ()
    {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }
    
    private void showToast (String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    
    private void addConversation (HashMap<String, Object> conversations)
    {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversations)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
    }
    
    private void updateConversation (String message, String messageType)
    {
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message
                , Constants.KEY_TIMESTAMP, new Date()
                , Constants.KEY_MESSAGE_TYPE, messageType
        );
    }
    
    private final EventListener<QuerySnapshot> eventListener = (value, error) ->
    {
        if ( error != null )
            return;
        
        if ( value != null )
        {
            int count = chatMessages.size();
            for ( DocumentChange documentChange : value.getDocumentChanges() )
            {
                if ( documentChange.getType() == DocumentChange.Type.ADDED )
                {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateAndTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.messageType = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if ( count == 0 )
                chatAdapter.notifyDataSetChanged();
            else
            {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if ( conversationId == null )
        {
            checkForConversationDriver();
        }
    };
    
    private void init ()
    {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                getBitmapFromEncodedString(receiverUser.image)
                , chatMessages
                , preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }
    
    private Bitmap getBitmapFromEncodedString (String encodedString)
    {
        if ( encodedString != null )
        {
            byte[] bytes = Base64.decode(encodedString, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        else
            return null;
    }
    
    private void loadReceiverDetails ()
    {
        receiverUser = (Users) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }
    
    private void setListeners ()
    {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.layoutSendMedia.setOnClickListener(v -> sendMedia());
    }
    
    private void sendMedia ()
    {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.launch(intent);
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
                                ProgressDialog progressDialog = new ProgressDialog(ChatActivity.this);
                                progressDialog.setCanceledOnTouchOutside(false);
                                progressDialog.show();
                                StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                                        .child(Constants.KEY_COLLECTION_IMAGE)
                                        .child(conversationId + "/")
                                        .child(imageUri.getLastPathSegment() + ".jpg");
                                storageReference.putFile(imageUri)
                                        .addOnSuccessListener(taskSnapshot ->
                                        {
                                            progressDialog.dismiss();
                                            storageReference.getDownloadUrl().addOnCompleteListener(task ->
                                            {
                                                String downloadLink = task.getResult().toString();
                                                HashMap<String, Object> message = new HashMap<>();
                                                message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                                                message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                                                message.put(Constants.KEY_MESSAGE, downloadLink);
                                                message.put(Constants.KEY_TIMESTAMP, new Date());
                                                message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_IS_IMAGE);
                                                database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
                                                
                                                recentConversation("Image", Constants.KEY_MESSAGE_TYPE_IS_IMAGE);
                                            });
                                        })
                                        .addOnFailureListener(e -> showToast("Image Upload Failed"))
                                        .addOnProgressListener(snapshot ->
                                        {
                                            int progress = (int) (100 * (snapshot.getBytesTransferred() / snapshot.getTotalByteCount()));
                                            progressDialog.setProgress(progress);
                                            progressDialog.setMessage("Uploaded\t" + progress + "%");
                                        });
                                
                            } catch ( Exception e )
                            {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
    );
    
    private String getReadableDateAndTime (Date date)
    {
        return new SimpleDateFormat("MMMM dd yyyy - hh:mm a", Locale.getDefault()).format(date);
    }
    
    private void checkForConversationDriver ()
    {
        if ( chatMessages.size() != 0 )
        {
            checkForConversation(
                    receiverUser.id
                    , preferenceManager.getString(Constants.KEY_USER_ID));
            checkForConversation(
                    preferenceManager.getString(Constants.KEY_USER_ID)
                    , receiverUser.id
            );
        }
    }
    
    private void checkForConversation (String senderId, String receiverId)
    {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }
    
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = task ->
    {
        if ( task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0 )
        {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
        
    };
    
    private void sendNotification (String messageBody)
    {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders()
                , messageBody
        ).enqueue(new Callback<String>()
        {
            @Override
            public void onResponse (@NonNull Call<String> call, @NonNull Response<String> response)
            {
                if ( response.isSuccessful() )
                {
                    try
                    {
                        if ( response.body() != null )
                        {
                            JSONObject responseJSON = new JSONObject(response.body());
                            JSONArray results = responseJSON.getJSONArray("results");
                            if ( responseJSON.getInt("failure") == 1 )
                            {
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                            }
                        }
                    } catch ( JSONException e )
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    showToast("Error : " + response.code());
                }
            }
            
            @Override
            public void onFailure (@NonNull Call<String> call, @NonNull Throwable t)
            {
                showToast(t.getMessage());
            }
        });
    }
    
    @Override
    protected void onResume ()
    {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}