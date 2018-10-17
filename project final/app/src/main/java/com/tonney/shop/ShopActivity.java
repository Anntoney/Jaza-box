package com.tonney.shop;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;


import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.tonney.shop.fragment.AboutFragment;
import com.tonney.shop.fragment.BestSellerFragment;
import com.tonney.shop.fragment.CategoryFragment;
import com.tonney.shop.fragment.DealsFragment;
import com.tonney.shop.fragment.NotificationFragment;
import com.tonney.shop.fragment.ShopHomeFragment;
import com.tonney.shop.utils.CustomApplication;
import com.tonney.shop.utils.DrawCart;
import com.tonney.notification.SettingsActivity;

public class ShopActivity extends AppCompatActivity{

    private static final String TAG = ShopActivity.class.getSimpleName();

    private FragmentManager fragmentManager;
    private Fragment fragment = null;

    private TextView cartBadge;

    private int mCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mCount = ((CustomApplication)getApplication()).cartItemCount();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        fragmentManager = getSupportFragmentManager();
        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragment = new ShopHomeFragment();
        fragmentTransaction.replace(R.id.content_shop, fragment);
        fragmentTransaction.commit();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        View header = navigationView.inflateHeaderView(R.layout.nav_header_shop);
        TextView profileName = (TextView) header.findViewById(R.id.profile_name);
        //profileName.setText("Adele");

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if(id == R.id.nav_Share){

                sharingActivity();

                }

                if (id == R.id.nav_explore) {
                    fragment = new ShopHomeFragment();
                }else if(id == R.id.nav_category){
                    fragment = new CategoryFragment();
                }else if(id == R.id.nav_best_seller){
                    fragment = new BestSellerFragment();
                }else if (id == R.id.nav_promotion) {
                    fragment = new DealsFragment();
                }else if (id == R.id.nav_notification) {
                    fragment = new NotificationFragment();
                }else if(id == R.id.nav_about){
                    fragment = new AboutFragment();
                }else if (id == R.id.nav_settings) {
                    Intent cartIntent = new Intent(ShopActivity.this, SettingsActivity.class);
                    startActivity(cartIntent);
                }else if (id == R.id.nav_accounts) {
                    Intent profileIntent = new Intent(ShopActivity.this, ProfileActivity.class);
                    startActivity(profileIntent);
                }else if (id == R.id.nav_logout) {
                    //remove user data from shared preference
                    ((CustomApplication)getApplication()).getShared().setUserData("");

                    Intent loginPageIntent = new Intent(ShopActivity.this, LoginActivity.class);
                    loginPageIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(loginPageIntent);
                    finish();
                }

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.replace(R.id.content_shop, fragment);
                transaction.commit();

                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                assert drawer != null;
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        //cartBadge = (TextView) MenuItemCompat.getActionView(navigationView.getMenu().findItem(R.id.nav_notification));
        //formatBadgeCounter(cartBadge);
        //cartBadge.setText("2");
    }

    private void sharingActivity() {


        Intent a = new Intent(Intent.ACTION_SEND);
        final String appPackageName = getApplicationContext().getPackageName();
        String strAppLink = "";

        try
        {
            strAppLink = "https://play.google.com/store/apps/details?id=" + appPackageName;
        }
        catch (android.content.ActivityNotFoundException anfe)
        {
            strAppLink = "https://play.google.com/store/apps/details?id=" + appPackageName;
        }
        // this is the sharing part
        a.setType("text/link");
        String shareBody = "Hey! Download by app for free and get amazing products." +
                "\n"+""+strAppLink;
        String shareSub = "APP NAME/TITLE";
        a.putExtra(Intent.EXTRA_SUBJECT, shareSub);
        a.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(a, "Share Using"));

    }

    private void formatBadgeCounter(TextView badge){
        badge.setWidth(96);
        badge.setGravity(Gravity.CENTER);
        setViewBackground(badge);
        badge.setTextColor(getResources().getColor(R.color.colorWhite));
    }

    private void setViewBackground(TextView badge){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            badge.setBackground( getResources().getDrawable(R.drawable.brown));
        }else{
            badge.setBackgroundDrawable( getResources().getDrawable(R.drawable.brown) );
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
            Intent optionIntent = new Intent(ShopActivity.this, LoginOptionActivity.class);
            startActivity(optionIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem menuItem = menu.findItem(R.id.action_shop);
        DrawCart dCart = new DrawCart(this);
        menuItem.setIcon(dCart.buildCounterDrawable(mCount, R.drawable.cart));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_shop) {
            Intent checkoutIntent = new Intent(ShopActivity.this, CartActivity.class);
            startActivity(checkoutIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        mCount = ((CustomApplication)getApplication()).cartItemCount();
        super.onResume();
    }
}
