package com.example.user.androidlabs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import com.example.user.androidlabs.database.UserProfile;
import com.example.user.androidlabs.database.UserRepository;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ProfileFragment.OnFragmentInteractionListener {

    private UserRepository userRepository;

    private NavController navController = null;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userRepository = new UserRepository();

        if (userRepository.getUser() == null){
            startAuthActivity();
            return;
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        navController = Navigation.findNavController(findViewById(R.id.nav_host_fragment));

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        setProfileEmail(userRepository.getEmail());
        navigationView.setNavigationItemSelectedListener(this);

        userRepository.addProfileEventListener(profileEventListener);

        updateNavImage();
    }

    private ValueEventListener profileEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            UserProfile userProfile = dataSnapshot.getValue(UserProfile.class);
            if (userProfile != null){
                getFullNameTextViewFromNavView().setText(
                        String.format("%s %s", userProfile.getFirstName(), userProfile.getLastName()));
            }
        }
        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            Log.d("ProfileImage", databaseError.getMessage());
        }
    };

    public void updateNavImage(){
        userRepository.getProfileImageBitmap()
                .addOnSuccessListener(successImageLoadListener)
                .addOnFailureListener(failureImageLoadListener);
    }

    private OnSuccessListener<byte[]> successImageLoadListener = new OnSuccessListener<byte[]>() {
        @Override
        public void onSuccess(byte[] bytes) {
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
            getProfileImageViewFromNavView().setImageBitmap(bmp);
        }
    };

    private OnFailureListener failureImageLoadListener = new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d("ProfileImage", exception.getMessage());
        }
    };

    public void hideKeyboard(){
        if(getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        hideKeyboard();

        int id = item.getItemId();
        int navigationId = 0;
        if (id == R.id.nav_home) {
            navigationId = R.id.homeFragment;
        } else if (id == R.id.nav_profile) {
            navigationId = R.id.profileFragment;
        } else if (id == R.id.nav_rss) {
            navigationId = R.id.rssFragment;
        }

        if (navigationId != 0){
            if (navController.getCurrentDestination().getId() == R.id.profileEditFragment) {
                NavigateToFragmentRequest(navigationId, null);
            } else {
                navController.navigate(navigationId);
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void startAuthActivity(){
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    private void setProfileEmail(String email) {
        View headerView = navigationView.getHeaderView(0);
        TextView textView = headerView.findViewById(R.id.nav_header_profile_email);
        textView.setText(email);
    }

    private TextView getFullNameTextViewFromNavView() {
        View headerView = navigationView.getHeaderView(0);
        return headerView.findViewById(R.id.nav_header_profile_fullname);
    }

    private ImageView getProfileImageViewFromNavView() {
        View headerView = navigationView.getHeaderView(0);
        return headerView.findViewById(R.id.nav_header_profile_image);
    }

    @Override
    public void onBackPressed() {
        if (navController.getCurrentDestination().getId() == R.id.profileEditFragment) {
            NavigateToFragmentRequest(R.id.profileFragment, null);
        } else {
            super.onBackPressed();
        }
    }

    private void startAboutActivity(){
        startActivity(new Intent(this, AboutActivity.class));
    }

    private void NavigateToFragmentRequest(final int fragmentId, final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.leave_from_profile_edit)
                .setPositiveButton(R.string.leave, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (context == null) {
                            navController.navigate(fragmentId);
                        }
                        else{
                            startAboutActivity();
                        }
                    }
                })
                .setNegativeButton(R.string.stay, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });
        builder.create().show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                if (navController.getCurrentDestination().getId() == R.id.profileEditFragment) {
                    NavigateToFragmentRequest(0, this);
                } else {
                    startAboutActivity();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
