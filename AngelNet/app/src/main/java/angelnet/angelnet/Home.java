package angelnet.angelnet;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import static com.google.android.gms.maps.model.BitmapDescriptorFactory.*;
import java.io.InputStream;
import java.lang.reflect.Field;

public class Home extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, View.OnClickListener{

    private int started;
    private String title;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private TextView AngelNet;
    private ImageView imageView;
    private SignInButton btnSignIn;
    private Button acknowledgements_btn;
    private ImageButton back_profile;
    private ImageButton back_about;
    private ImageButton back_acknowledgements;
    private TextView textView_name, textView_email;
    private RelativeLayout map_layout;
    private RelativeLayout profile_layout;
    private RelativeLayout about_layout;
    private RelativeLayout acknowledgements_layout;
    private ImageView imageView_profile_image;
    private boolean mIntentInProgress;
    private boolean mSignInClicked;
    private ConnectionResult mConnectionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disableShowHideAnimation(getSupportActionBar());
        getSupportActionBar().hide();
        title = "Menu";
        getSupportActionBar().setTitle(title);
        setContentView(R.layout.activity_home);
        btnSignIn = (SignInButton) findViewById(R.id.btn_sign_in);
        setGooglePlusButtonText(btnSignIn, "Sign in with Google");
        started=0;//to not log in multiple time, important
        AngelNet = (TextView) findViewById(R.id.AngelNet);
        imageView = (ImageView) findViewById(R.id.imageView);
        btnSignIn.setOnClickListener(this);
        acknowledgements_btn = (Button) findViewById(R.id.acknowledgements_btn);
        acknowledgements_btn.setOnClickListener(this);
        back_profile = (ImageButton) findViewById(R.id.back_profile);
        back_profile.setOnClickListener(this);
        back_about = (ImageButton) findViewById(R.id.back_about);
        back_about.setOnClickListener(this);
        back_acknowledgements = (ImageButton) findViewById(R.id.back_acknowledgements);
        back_acknowledgements.setOnClickListener(this);
        imageView_profile_image = (ImageView) findViewById(R.id.profile_image);
        textView_name = (TextView) findViewById(R.id.textView_name);
        textView_email = (TextView) findViewById(R.id.textView_email);
        map_layout = (RelativeLayout) findViewById(R.id.map_layout);
        profile_layout = (RelativeLayout) findViewById(R.id.profile_layout);
        about_layout = (RelativeLayout) findViewById(R.id.about_layout);
        acknowledgements_layout = (RelativeLayout) findViewById(R.id.acknowledgements_layout);
        // Initializing google plus api client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN).build();
    }

    /*


    Google Sign-In Code


    */

    // Changes text of sign-in button
    protected void setGooglePlusButtonText(SignInButton signInButton, String buttonText) {
        for (int i = 0; i < signInButton.getChildCount(); i++) {
            View v = signInButton.getChildAt(i);
            if (v instanceof TextView) {
                TextView tv = (TextView) v;
                tv.setText(buttonText);
                return;
            }
        }
    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if(started!=1) {
            mSignInClicked = false;
            Toast toast = Toast.makeText(this, "           User is connected!\nWelcome to the requests page!", Toast.LENGTH_LONG);
            toast.show();
            updateUI(true);
            // Get user's information
            getProfileInformation();
            started=1;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
        updateUI(false);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this,
                    0).show();
            return;
        }
        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = connectionResult;
            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }
    }

    private static final int GOOGLE_SIGIN = 100;
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, GOOGLE_SIGIN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode,
                                    Intent intent) {
        if (requestCode == GOOGLE_SIGIN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;
            }
            mIntentInProgress = false;
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            //Initialize the map
            try {
                initilizeMap();
                disableShowHideAnimation(getSupportActionBar());
                getSupportActionBar().show();
                double latitude = 0;
                double longitude = 0;
                GPSTracker gps;
                gps = new GPSTracker(this);
                if(gps.canGetLocation()) {
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                    MarkerOptions marker = new MarkerOptions().position(
                            new LatLng(latitude, longitude))
                            .title("Request");
                    marker.icon(
                            defaultMarker(HUE_RED));
                    mMap.addMarker(marker);
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(latitude,
                                    longitude)).zoom(17).build();
                    mMap.animateCamera(CameraUpdateFactory
                            .newCameraPosition(cameraPosition));
                }else{
                    gps.showSettingsAlert();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            AngelNet.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            btnSignIn.setVisibility(View.GONE);
            map_layout.setVisibility(View.VISIBLE);
        } else {
            AngelNet.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.VISIBLE);
            btnSignIn.setVisibility(View.VISIBLE);
			mMap.clear();
            map_layout.setVisibility(View.GONE);
            disableShowHideAnimation(getSupportActionBar());
            getSupportActionBar().hide();
            started=0;
        }
    }

    private void getProfileInformation() {
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi
                        .getCurrentPerson(mGoogleApiClient);
                String personName = currentPerson.getDisplayName();
                String personPhotoUrl = currentPerson.getImage().getUrl();
                String personGooglePlusProfile = currentPerson.getUrl();
                String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
                textView_name.setText(personName);
                textView_email.setText(email);
                // by default the profile url gives 50x50 px image only
                // we can replace the value with whatever dimension we want by
                // replacing sz=X
                personPhotoUrl = personPhotoUrl.substring(0,
                        personPhotoUrl.length() - 2)
                        + 400;
                new LoadProfileImage(imageView_profile_image).execute(personPhotoUrl);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
      Background Async task to load user profile picture from url
    */
    private class LoadProfileImage extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public LoadProfileImage(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    /*


    Clickables Code


    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    public static void disableShowHideAnimation(ActionBar actionBar) {
        try
        {
            actionBar.getClass().getDeclaredMethod("setShowHideAnimationEnabled", boolean.class).invoke(actionBar, false);
        }
        catch (Exception exception)
        {
            try {
                Field mActionBarField = actionBar.getClass().getSuperclass().getDeclaredField("mActionBar");
                mActionBarField.setAccessible(true);
                Object icsActionBar = mActionBarField.get(actionBar);
                Field mShowHideAnimationEnabledField = icsActionBar.getClass().getDeclaredField("mShowHideAnimationEnabled");
                mShowHideAnimationEnabledField.setAccessible(true);
                mShowHideAnimationEnabledField.set(icsActionBar,false);
                Field mCurrentShowAnimField = icsActionBar.getClass().getDeclaredField("mCurrentShowAnim");
                mCurrentShowAnimField.setAccessible(true);
                mCurrentShowAnimField.set(icsActionBar,null);
            }catch (Exception e){
                //....
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem v) {
        switch (v.getItemId()) {
            case R.id.action_profile:
                // profile set button clicked
                setProfileScreen();
                break;
            case R.id.action_about:
                // about set button clicked
                setAboutScreen();
                break;
            case R.id.action_logout:
                // logout button clicked
                signOutFromGplus();
                break;
        }
        return true;
    }

    private void setProfileScreen() {
        map_layout.setVisibility(View.GONE);
        disableShowHideAnimation(getSupportActionBar());
        getSupportActionBar().hide();
        profile_layout.setVisibility(View.VISIBLE);
    }

    private void setAboutScreen() {
        map_layout.setVisibility(View.GONE);
        disableShowHideAnimation(getSupportActionBar());
        getSupportActionBar().hide();
        about_layout.setVisibility(View.VISIBLE);
    }

    private void signOutFromGplus() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            updateUI(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sign_in:
                // sign-in button clicked
                signInWithGplus();
                break;
            case R.id.acknowledgements_btn:
                // about set button clicked
                setAcknowledgementsScreen();
                break;
            case R.id.back_profile:
                // back button clicked
                backProfileFunction();
                break;
            case R.id.back_about:
                // back button clicked
                backAboutFunction();
                break;
            case R.id.back_acknowledgements:
                // back button clicked
                backAcknowledgementsFunction();
                break;
        }
    }

    private void signInWithGplus() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignInClicked = true;
            resolveSignInError();
        }
    }

    private void setAcknowledgementsScreen() {
        about_layout.setVisibility(View.GONE);
        acknowledgements_layout.setVisibility(View.VISIBLE);
    }

    private void backProfileFunction() {
        profile_layout.setVisibility(View.GONE);
        disableShowHideAnimation(getSupportActionBar());
        getSupportActionBar().show();
        map_layout.setVisibility(View.VISIBLE);
    }

    private void backAboutFunction() {
        about_layout.setVisibility(View.GONE);
        disableShowHideAnimation(getSupportActionBar());
        getSupportActionBar().show();
        map_layout.setVisibility(View.VISIBLE);
    }

    private void backAcknowledgementsFunction() {
        acknowledgements_layout.setVisibility(View.GONE);
        about_layout.setVisibility(View.VISIBLE);
    }

    /*


    GoogleMap Code


    */

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initilizeMap() {

        if (mMap == null) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            // Showing / hiding your current location
            mMap.setMyLocationEnabled(true);

            // Enable / Disable zooming controls
            mMap.getUiSettings().setZoomControlsEnabled(true);

            // Enable / Disable my location button
            mMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Enable / Disable Compass icon
            mMap.getUiSettings().setCompassEnabled(true);

            // Enable / Disable Rotate gesture
            mMap.getUiSettings().setRotateGesturesEnabled(true);

            // Enable / Disable zooming functionality
            mMap.getUiSettings().setZoomGesturesEnabled(true);

            // check if map is created successfully or not
            if (mMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Sorry, unable to create map!", Toast.LENGTH_LONG)
                        .show();
            }
        }
    }
}
