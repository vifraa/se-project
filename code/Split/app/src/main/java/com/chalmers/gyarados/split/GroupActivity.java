package com.chalmers.gyarados.split;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.chalmers.gyarados.split.model.Message;
import com.chalmers.gyarados.split.model.User;

import java.util.List;


public class GroupActivity extends AppCompatActivity implements ClientListener, ProfileFragment.OnFragmentInteractionListener {

    //-------------LOGGING---------------------------
    /**
     * Used for logging
     */
    private static final String TAG = "ClientActivity";


    //------------------GUI-------------------------------
    /**
     * Used to show messages
     */
    private RecyclerView mMessageRecycler;
    /**
     * Used by the message recycler
     */
    private MessageListAdapter mMessageAdapter;

    /**
     * Where the user can write his messages
     */
    private EditText writtenText;
    /**
     * Used th show an loading animation while finding a group
     */
    private ViewDialog viewDialog;

    //private TextView groupMembers;

    private LinearLayout buttonHolder;

    //------------------OTHER PROPERTIES------------------------------------

    /**
     * USed to connect to server and send messages
     */
    private Client client;


    private boolean fromMainActivity;
    private String groupID;

    //-------------------ANDROID METHODS---------------------------------------------
    /**
     * The method that creates the activity.
     * Properties are created, gui elements are initialized and we try to establish a connection to the websocket.
     * @param savedInstanceState not used
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_view);

        buttonHolder = findViewById(R.id.button_holder);

        //initializing gui
        writtenText = findViewById(R.id.writtenText);
        //groupMembers=findViewById(R.id.groupMembers);
        ImageButton sendButton = findViewById(R.id.sendbutton);
        ImageButton leaveButton = findViewById(R.id.leaveButton);
        sendButton.setOnClickListener(v -> onSendButtonPressed(writtenText.getText().toString()));
        leaveButton.setOnClickListener(l -> onLeaveButtonPressed());

        viewDialog = new ViewDialog(this);
        showCustomLoadingDialog();
        //Retrieving the groupID that might have been given by activity before
        groupID=getIntent().getStringExtra("groupID");
        if(groupID!=null){
            client=new Client(groupID,this);
        }else{
            fromMainActivity=true;
            client = new Client(this);
        }

        client.connectStomp();
    }

    /**
     * When the activity is to be destroyed the client will disconnect from the server.
     * We will also dispose all our current subscriptions.
     */
    @Override
    protected void onDestroy() {
        client.disconnect();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }

    //-------------RECEIVING MESSAGE-------------------------------
    /**
     * This method is called when the user receives a new message that belongs to the group
     * @param message
     */
    public void newGroupMessageReceived(Message message) {
        hideCustomDialogIfNeeded();
        if(message.getType().equals(MessageType.JOIN )){
            client.askForUserInfo(message.getSender());
        }else if(message.getType().equals(MessageType.LEAVE)){
            removeProfileButton(message.getSender());
        }
        mMessageAdapter.addItem(message);

        mMessageRecycler.scrollToPosition(mMessageAdapter.getItemCount()-1);
    }



    //-------------SENDING MESSAGE------------------------------


    private void sendChatMessage(Message message){
        client.sendMessageToChat(message);
    }



    //-----------------LEAVING----------------------------------------
    
    private void transferToRatingView(){
        client.leaveGroup();
        Intent intent = new Intent(GroupActivity.this,RatingActivity.class);
        intent.putExtra("GroupID", client.getGroupId());
        startActivity(intent);
    }

    //-----------------GUI METHODS----------------------------------

    /**
     * Initialises the message recycler and the message adapter. Adds the given messages to the adapter.
     * @param messages The messages that are to be added to the message view.
     */
    private void initMessageView(List<Message> messages) {
        mMessageRecycler = (RecyclerView) findViewById(R.id.reyclerview_message_list);
        mMessageAdapter = new MessageListAdapter(this, messages);
        mMessageRecycler.setLayoutManager(new LinearLayoutManager(this));
        mMessageRecycler.setAdapter(mMessageAdapter);
    }
    private void hideCustomDialogIfNeeded() {
        if(viewDialog.isShowing()){
            hideCustomLoadingDialog();
        }
    }

    public void showCustomLoadingDialog() {

        //..show gif
        viewDialog.showDialog();
    }

    public void hideCustomLoadingDialog(){
        viewDialog.hideDialog();
    }
    /**
     * Sends a message to the earlier initialized group number.
     * @param message The message that are to be sent to the server
     */
    public void onSendButtonPressed(String message){
        if(message!=null && !message.isEmpty()){
            sendChatMessage(new Message(message,CurrentSession.getCurrentUser(),MessageType.CHAT));
            clearSendText();
        }
    }

    private void clearSendText() {
        writtenText.getText().clear();
    }

    public void onLeaveButtonPressed(){
        transferToRatingView();
        //returnToPreviousActivity();
    }

    public void updateMembersList(List<User> users) {
        for(User u:users){
            client.askForUserInfo(u);
        }
    }

    private void addProfileButton(User u) {
        ProfileButton button = new ProfileButton(getApplicationContext(),null,u);
        buttonHolder.addView(button);
        button.setOnClickListener(new ClickListener(u));
    }

    private void removeProfileButton(User user) {
        int size = buttonHolder.getChildCount();
        for(int i = 0; i<size;i++){
            ProfileButton b = (ProfileButton)buttonHolder.getChildAt(i);
            if (b.getUser().getUserId().equals(user.getUserId())){
                buttonHolder.removeViewAt(i);
                break;
            }
        }

    }

    private class ClickListener implements View.OnClickListener {
        private User user;

        public ClickListener(User user) {
            this.user = user;
        }

        @Override
        public void onClick(View v) {
            ProfileFragment fragment = ProfileFragment.newInstance();
            fragment.setUser(user);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.addToBackStack(null);
            transaction.replace(R.id.fragmentholder, fragment).commit();
            findViewById(R.id.fragmentholder).bringToFront();
        }
    }
    //-------------INITIALIZING---------------------------------------


    /**
     * Called when client has found a group and received all old messages
     * @param messages
     */
    @Override
    public void onOldMessagesReceived(List<Message> messages) {
        initMessageView(messages);
    }

    /**
     * Called when the client is ready.
     */
    @Override
    public void onClientReady() {
        hideCustomDialogIfNeeded();
    }

    @Override
    public void userInfoReceived(User user) {
        addProfileButton(user);
    }




    //-------------------------ERROR HANDLING------------------------------

    @Override
    public void errorOnLifeCycleFirstConnect() {
        hideCustomDialogIfNeeded();
        client.disconnect();
        returnToPreviousActivity();
    }

    @Override
    public void onConnectionClosedFirstConnect() {
        hideCustomDialogIfNeeded();
        client.disconnect();
        returnToPreviousActivity();
    }

    @Override
    public void onConnectionClosed() {
        //todo what if this happens....
    }

    public void errorWhileSendingMessage(Throwable throwable) {
        hideCustomDialogIfNeeded();
        Log.e(TAG, "Error while sending message", throwable);
        //todo what if this happens...

    }


    public void errorOnSubcribingOnTopic(Throwable throwable) {
        hideCustomDialogIfNeeded();
        Log.e(TAG, "Error on subscribe topic", throwable);
        //todo what if this happens...
        //client.disconnect();
        //returnToPreviousActivity();

    }
    public void errorOnLifeCycle() {
        //todo what if this happens...
    }


    private void returnToPreviousActivity(){
        if(fromMainActivity){
            finish();
        }else{
            Intent intent = new Intent(GroupActivity.this,MainActivity.class);
            finish();
            startActivity(intent);
        }

    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
