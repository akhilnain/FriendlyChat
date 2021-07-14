
package com.example.friendlychat;
        import androidx.annotation.NonNull;
        import androidx.annotation.Nullable;
        import androidx.appcompat.app.AppCompatActivity;
        import android.os.Bundle;
        import android.text.Editable;
        import android.text.InputFilter;
        import android.text.TextWatcher;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.View;
        import android.widget.Button;
        import android.widget.EditText;
        import android.widget.ImageButton;
        import android.widget.ListView;
        import android.widget.ProgressBar;
        import android.widget.Toast;

        import com.firebase.ui.auth.AuthUI;
        import com.google.firebase.auth.FirebaseAuth;
        import com.google.firebase.auth.FirebaseUser;
        import com.google.firebase.database.ChildEventListener;
        import com.google.firebase.database.DataSnapshot;
        import com.google.firebase.database.DatabaseError;
        import com.google.firebase.database.DatabaseReference;
        import com.google.firebase.database.FirebaseDatabase;

        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mFirebaseDatabaseReference;
    private ChildEventListener mChildEventListner;

    private FirebaseAuth mFirebaseAuth;
    public static final int RC_SIGN_IN=1;//this flag var for Firebase UI auth

    //It will help ti know user state thta he is login or not and do different things at  each time
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;


        mFirebaseDatabase=FirebaseDatabase.getInstance();
        mFirebaseAuth=FirebaseAuth.getInstance();
        mFirebaseDatabaseReference=mFirebaseDatabase.getReference().child("messages");

        // Initialize references to views
        mProgressBar = findViewById(R.id.progressBar);
        mMessageListView = findViewById(R.id.messageListView);
        mPhotoPickerButton = findViewById(R.id.photoPickerButton);
        mMessageEditText = findViewById(R.id.messageEditText);
        mSendButton = findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mSendButton.setEnabled(charSequence.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                //Creating a instace of FriendlyMessage Class to store Message Data in Friendly Message Variable
                //passing text as message , mUsername as username , photoUrl as null for now
                FriendlyMessage friendlyMessage = new FriendlyMessage
                        (mMessageEditText.getText().toString(), mUsername, null);
                //this it to pass a values to the messgae database of our app
                mFirebaseDatabaseReference.push().setValue(friendlyMessage);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

        mAuthStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged( FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser();
                if(user !=null){
                    //when signed in
                    onSignedoutInInitialize(user.getDisplayName());
                    Toast.makeText(MainActivity.this, "Signed in", Toast.LENGTH_SHORT).show();

                }else{
                    //when signed out
                    onSignedOutCleanup();
                    //Firebase Ui auth
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build() ))
                                    .build(),
                            RC_SIGN_IN);
                }

            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        mMessageAdapter.clear();
        detachDatabaseReasListener();
    }
    private void onSignedoutInInitialize(String username){
        mUsername=username;
        attachDatabaseReasListener();
    }
    private void onSignedOutCleanup(){
        mUsername=ANONYMOUS;
        mMessageAdapter.clear();
        detachDatabaseReasListener();

    }
    private void attachDatabaseReasListener(){
        if(mChildEventListner == null){
        mChildEventListner=new ChildEventListener() {
            @Override
            public void onChildAdded( DataSnapshot snapshot,  String previousChildName) {
                //this function autumaticly called when new child is added or for fitrst time for every child present

                //we are fetching data in friendly message class as data is stored in that way
                FriendlyMessage friendlyMessage=snapshot.getValue(FriendlyMessage.class);

                //we have set it to the custom adaptor created for listview
                mMessageAdapter.add(friendlyMessage);

            }

            @Override
            public void onChildChanged( DataSnapshot snapshot, String previousChildName) { }
            @Override
            public void onChildRemoved(DataSnapshot snapshot) { }
            @Override
            public void onChildMoved( DataSnapshot snapshot,  String previousChildName) { }
            @Override
            public void onCancelled( DatabaseError error) { }
        };
        mFirebaseDatabaseReference.addChildEventListener(mChildEventListner);}
    }
    private void detachDatabaseReasListener(){
        if(mChildEventListner != null){
        mFirebaseDatabaseReference.removeEventListener(mChildEventListner);
        mChildEventListner=null;
    }}
}