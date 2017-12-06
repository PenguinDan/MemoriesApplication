package edu.csulb.memoriesapplication;

import android.*;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Created by Danie on 10/16/2017.
 */

public class LatestMemoriesActivity extends AppCompatActivity {
    //added
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView recyclerView;
    private ArrayList<Polaroid> polaroids;
    private CardViewAdapter rvAdapter;
    private MyConstraintLayout constraintLayout;
    private ArrayDeque<String> urlList;
    private boolean accessLocationPermission;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int PERMISSION_REQUEST_CODE = 1052;
    private String TAG = "LatestMemoriesActivity";
    private SimpleExoPlayerView currentlyPlayingVideo;
    private String mCurrentPhotoPath;
    private boolean cameraRequest;

    /*TODO: Card Views appear after the query finishes, have them actually be there right when the activity starts
    TODO:Populate those card views after the fact of the above and do the same for trending activity
    TODO: Add the location title on top of the polaroids, I will query them in once I get the location attribute attachment finished
     */

    //Listener that is attached to the query
    ChildEventListener childEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            //Receive the url string and store it in a list
            String urlString = (String) dataSnapshot.child(GlobalDatabase.URL_KEY).getValue();
            String mediaType = (String) dataSnapshot.child(GlobalDatabase.MEDIA_TYPE_KEY).getValue();
            if(mediaType.charAt(0) == 'i'){
                urlString = urlString + 'i';
            } else if(mediaType.charAt(0) == 'v') {
                urlString = urlString + 'v';
            }
            urlList.addFirst(urlString);
            Log.d(TAG, urlString);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            loadPolaroids();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trending);

        //set transitions
        setTransitions();

        //Variable to check if the user wants a camera request to change the behavior of onRequestPermissionsResult
        cameraRequest = false;

        //Check if we are able to access the user's location
        accessLocationPermission = UserPermission.checkUserPermission(this, UserPermission.Permission.LOCATION_PERMISSION);

        //If accessLocationPermission variable is false, cannot display anything... ask for user's permission
        if(!accessLocationPermission) {
            requestPermission();
        } else {
            //Application has permission to use the user's location, initialize the query
            initializeQuery();
        }

        //instantiate objects
        constraintLayout = (MyConstraintLayout) findViewById(R.id.constraintLayout);
        Intent startNeighborActivity = new Intent(this, TrendingActivity.class);
        startNeighborActivity.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        constraintLayout.setLeftPage(startNeighborActivity);
        polaroids = new ArrayList<Polaroid>();
        rvAdapter = new CardViewAdapter(this, polaroids);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(this.getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(rvAdapter);
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                int position1 = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
                int position2 = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                Log.d("Latest Memories/first completely visible item position", "" + position1);
                if (position1 != -1) {
                    View view = (View) ((LinearLayoutManager) recyclerView.getLayoutManager()).findViewByPosition(position1);
                    if (view instanceof CardView) {
                        Log.d("View is of type", "CardView");
                        View childView = (View) ((CardView) view).getChildAt(0);
                        if (childView instanceof RelativeLayout) {
                            SimpleExoPlayerView videoView = (SimpleExoPlayerView) ((RelativeLayout) childView).getChildAt(0);
                            SimpleExoPlayer simpleExoPlayer = videoView.getPlayer();
                            simpleExoPlayer.setPlayWhenReady(true);
                            if(currentlyPlayingVideo != null && currentlyPlayingVideo != videoView){
                                SimpleExoPlayer playerToPause = currentlyPlayingVideo.getPlayer();
                                playerToPause.setPlayWhenReady(false);
                            }
                            currentlyPlayingVideo = videoView;
                        }
                        else if(currentlyPlayingVideo != null){
                            SimpleExoPlayer playerToPause = currentlyPlayingVideo.getPlayer();
                            playerToPause.setPlayWhenReady(false);
                        }

                    }
                    if (position2 != -1 && position2 != position1) {
                        Log.d("first visible item postion", "" + position2);
                        View view2 = (View) ((LinearLayoutManager) recyclerView.getLayoutManager()).findViewByPosition(position2);
                        View childView = (View) ((CardView) view2).getChildAt(0);
                        if (childView instanceof RelativeLayout) {
                            SimpleExoPlayerView videoView = (SimpleExoPlayerView) ((RelativeLayout) childView).getChildAt(0);
                            SimpleExoPlayer simpleExoPlayer = videoView.getPlayer();
                            simpleExoPlayer.setPlayWhenReady(false);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        // Override search hint
        searchView.setQueryHint(getResources().getString(R.string.search_hint));

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem items) {
        switch (items.getItemId()) {
            case R.id.action_settings: {
                final Intent intent = new Intent(LatestMemoriesActivity.this, Setting.class);
                startActivity(intent);
                Toast.makeText(getBaseContext(), "Information: ", Toast.LENGTH_SHORT).show();
                return true;
            }
            case R.id.action_cam: {
                if(accessLocationPermission) {
                    dispatchTakePictureIntent();
                    return true;
                } else {
                    cameraRequest = true;
                    requestPermission();
                }
            }
        }
        return super.onOptionsItemSelected(items);
    }

    //Asks for the user's permission, double check just in case, don't want to ask the user a second time
    private void requestPermission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    //Permission result received, act accordingly
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission has been granted
                    accessLocationPermission = true;
                    if(cameraRequest) {
                        dispatchTakePictureIntent();
                        cameraRequest = false;
                    }
                } else {
                    //Permission Denied
                    Toast.makeText(this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "edu.csulb.memoriesapplication.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, "tempImage.jpg");
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            Intent sendIntent = new Intent(LatestMemoriesActivity.this, DisplayActivity.class);

            sendIntent.putExtra("filepath", mCurrentPhotoPath);

            startActivity(sendIntent);
        }
    }



    public void setTransitions() {
        Slide enterSlide = new Slide();
        Slide exitSlide = new Slide();
        enterSlide.setDuration(500);
        enterSlide.setSlideEdge(Gravity.RIGHT);
        exitSlide.setDuration(500);
        exitSlide.setSlideEdge(Gravity.START);
        getWindow().setExitTransition(exitSlide);
        getWindow().setEnterTransition(enterSlide);
        getWindow().setReenterTransition(enterSlide);
        getWindow().setReturnTransition(enterSlide);
    }

    //Retrieves a list of url links and returns null for an empty list
    private void initializeQuery() {
        //Creates a reference for the location where every media link is stored ordered by time
        DatabaseReference databaseReference = GlobalDatabase.getMediaListReference("hi");
        //Set the maximum amount of queries to be received at once
        int maxQuerySize = 30;
        //Initialize the query located as a private class variable
        Query urlQuery = databaseReference.limitToFirst(maxQuerySize);
        /*
        Attach a listener so that if any more media links are added to the database,
        they will be added to the top of the array list stack*/
        urlQuery.addChildEventListener(childEventListener);
        //Add listener so that we know the update finished
        urlQuery.addValueEventListener(valueEventListener);
        //Initialize the ArrayList to hold the url strings
        urlList = new ArrayDeque<>(maxQuerySize);
    }

    private void loadPolaroids(){
        String combinedString;
        String uriString;
        char uriType;
        int size = urlList.size();
        Log.d("urlList size", "" + size);

        for(int i = 0; i < size; i++){
            combinedString = urlList.poll();
            Log.d("Combined String", combinedString);
            uriString = combinedString.substring(0, combinedString.length() - 2);
            Log.d("uriString", uriString);
            uriType = combinedString.charAt(combinedString.length() -1);
            Log.d("uriType", ""+ uriType);
            if(uriType == 'i'){
                polaroids.add(new Polaroid(null, Uri.parse(uriString)));
            }
            else if(uriType == 'v'){
                Log.d("Latest loadPolaroid()", "loading video");
                polaroids.add(new Polaroid(Uri.parse(uriString), null));
            }
            rvAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(currentlyPlayingVideo != null){
            SimpleExoPlayer playerToPause = currentlyPlayingVideo.getPlayer();
            playerToPause.setPlayWhenReady(false);
        }
    }

}
