package com.speakeasy.Android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.google.gson.annotations.SerializedName;
import com.speakeasy.Android.auth.LoginActivity;
import com.speakeasy.Android.common.ApiBaseResponse;
import com.speakeasy.Android.common.ApiConstants;
import com.speakeasy.Android.common.ApiUsersMeRequest;
import com.speakeasy.Android.common.ChooserIntent;
import com.speakeasy.Android.common.Api;
import com.speakeasy.Android.common.model.ApiAccessToken;
import com.speakeasy.Android.common.model.ApiUser;
import com.speakeasy.Android.common.persist.ObjectAsFile;
import com.speakeasy.Android.common.persist.Row;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        View.OnClickListener {

    public static final String EXTRA_ACCESS_TOKEN = "access_token";
    public static final String EXTRA_URL = "url";
    private static final String STATE_NAVIGATION_ROWS = "navigation_rows";
    private static final String STATE_USER = "user";
    private static final int RC_SELECT_AVATAR = 1;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private ProgressBar mProgressBar;

    private NavigationView mNavigationView;
    private ImageView mHeaderImg;
    private TextView mHeaderTxt;
    private ProgressBar mHeaderProgress;

    private FloatingActionButton mPost;
    private final Map<String, String> mPostLinks = new HashMap<>();

    private ApiAccessToken mAccessToken;
    private ArrayList<Row> mNavigationRows = new ArrayList<>();
    private ApiUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
        mNavigationView.setNavigationItemSelectedListener(this);
        mDrawerLayout.openDrawer(mNavigationView);

        // TODO: revert programmatically inflating the header view when the bug is fixed
        // https://code.google.com/p/android/issues/detail?id=190226
        View headerLayout = mNavigationView.inflateHeaderView(R.layout.drawer_header);
        mHeaderImg = (ImageView) headerLayout.findViewById(R.id.header_img);
        mHeaderImg.setOnClickListener(this);
        mHeaderTxt = (TextView) headerLayout.findViewById(R.id.header_txt);
        mHeaderProgress = (ProgressBar) headerLayout.findViewById(R.id.header_progress);

        mPost = (FloatingActionButton) findViewById(R.id.post);
        mPost.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SELECT_AVATAR:
                if (resultCode == RESULT_OK) {
                    Uri avatarUri = ChooserIntent.getUriFromChooser(this, data);

                    if (avatarUri != null) {
                        try {
                            new UsersMeAvatarRequest(mAccessToken,
                                    ChooserIntent.getFileNameFromUri(this, avatarUri),
                                    getContentResolver().openInputStream(avatarUri)).start();

                            mHeaderImg.setImageURI(avatarUri);
                            mHeaderProgress.setVisibility(View.VISIBLE);
                        } catch (FileNotFoundException e) {
                            Log.e(getClass().getSimpleName(), e.toString());
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent mainIntent = getIntent();
        if (mainIntent != null && mainIntent.hasExtra(EXTRA_ACCESS_TOKEN)) {
            mAccessToken = (ApiAccessToken) mainIntent.getSerializableExtra(EXTRA_ACCESS_TOKEN);
        }

        if (mAccessToken != null) {
            if (mNavigationRows.size() == 0) {
                new IndexRequest(mAccessToken).start();
            }

            if (mUser == null) {
                new UsersMeRequest(mAccessToken).start();
            }

            if (mainIntent != null && mainIntent.hasExtra(EXTRA_URL)) {
                String url = mainIntent.getStringExtra(EXTRA_URL);
                if (!TextUtils.isEmpty(url)) {
                    addDataFragment(url, mAccessToken, true);
                }
            }
        } else {
            startLoginActivity();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(STATE_NAVIGATION_ROWS, mNavigationRows);
        outState.putSerializable(STATE_USER, mUser);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_NAVIGATION_ROWS)) {
            ArrayList<Row> rows = savedInstanceState.getParcelableArrayList(STATE_NAVIGATION_ROWS);
            if (rows != null) {
                setNavigationRows(rows);
            }
        }

        if (savedInstanceState.containsKey(STATE_USER)) {
            setUser((ApiUser) savedInstanceState.getSerializable(STATE_USER));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Row row = mNavigationRows.get(menuItem.getItemId());
        if (row != null) {
            addDataFragment(row.value, null, true);
            mDrawerLayout.closeDrawer(mNavigationView);
            return true;
        }

        return false;
    }

    @Override
    public void onClick(View v) {
        if (v == mHeaderImg) {
            Intent chooserIntent = ChooserIntent.create(this, R.string.avatar_pick_image, "image/*");
            startActivityForResult(chooserIntent, RC_SELECT_AVATAR);
        } else if (v == mPost) {
            FragmentManager fm = getSupportFragmentManager();
            List<Fragment> fs = fm.getFragments();
            if (fm.getBackStackEntryCount() == 1
                    && fs.size() > 0
                    && fs.get(0) instanceof PostFragment) {
                // a post fragment is active, submit it now
                PostFragment postFragment = (PostFragment) fs.get(0);
                postFragment.submit();
                return;
            }

            // show dialog for user to choose a content type to post
            final String[] keys = mPostLinks.keySet().toArray(new String[mPostLinks.size()]);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.post_prompt)
                    .setItems(keys,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String url = mPostLinks.get(keys[which]);
                                    if (url != null) {
                                        Fragment fragment = PostFragment.newInstance(url, mAccessToken);
                                        addFragmentToBackStack(fragment, true);
                                    }
                                }
                            })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.are_you_sure_quit)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    private void startLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);

        Intent mainIntent = getIntent();
        if (mainIntent != null && mainIntent.hasExtra(EXTRA_URL)) {
            loginIntent.putExtra(LoginActivity.EXTRA_REDIRECT_TO, mainIntent.getStringExtra(EXTRA_URL));
        }

        startActivity(loginIntent);
        finish();
    }

    private void setNavigationRows(ArrayList<Row> rows) {
        mNavigationRows = rows;

        Menu menu = mNavigationView.getMenu();
        menu.clear();

        for (int i = 0; i < mNavigationRows.size(); i++) {
            menu.add(Menu.NONE, i, Menu.NONE, mNavigationRows.get(i).key);
        }
    }

    private void setUser(ApiUser u) {
        mUser = u;

        if (mUser != null) {
            Glide.with(this)
                    .load(mUser.getAvatar())
                    .placeholder(R.drawable.avatar_l)
                    .into(mHeaderImg);
            mHeaderTxt.setText(mUser.getUsername());
        } else {
            mHeaderImg.setImageDrawable(null);
            mHeaderTxt.setText("");
        }

        mHeaderProgress.setVisibility(View.GONE);
    }

    public void setTheProgressBarVisibility(boolean visible) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void addDataFragment(String url, ApiAccessToken at, boolean clearStack) {
        Fragment fragment = DataFragment.newInstance(url, at);
        addFragmentToBackStack(fragment, clearStack);
    }

    public void addFragmentToBackStack(Fragment fragment, boolean clearStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (clearStack) {
            // http://stackoverflow.com/questions/6186433/clear-back-stack-using-fragments
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        String tag = String.valueOf(fragmentManager.getBackStackEntryCount());
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    public ApiAccessToken getAccessToken() {
        return mAccessToken;
    }

    public ApiUser getUser() {
        return mUser;
    }

    class IndexRequest extends Api.GetRequest {
        IndexRequest(ApiAccessToken at) {
            super(ApiConstants.URL_INDEX, new Api.Params(at));
        }

        @Override
        protected void onStart() {
            mPost.setVisibility(View.GONE);
            mPostLinks.clear();
        }

        @Override
        protected void onSuccess(String response) {
            IndexResponse data = App.getGsonInstance().fromJson(response, IndexResponse.class);

            if (data.links != null) {
                ArrayList<Row> rows = new ArrayList<>(data.links.size());

                for (Map.Entry<String, String> link : data.links.entrySet()) {
                    final Row row = new Row();
                    row.key = link.getKey();
                    row.value = link.getValue();
                    rows.add(row);
                }

                setNavigationRows(rows);
            }

            if (data.post != null) {
                for (Map.Entry<String, String> post : data.post.entrySet()) {
                    mPostLinks.put(post.getKey(), post.getValue());
                }
                if (mPostLinks.size() > 0) {
                    mPost.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    class UsersMeRequest extends ApiUsersMeRequest {
        UsersMeRequest(ApiAccessToken at) {
            super(new Listener() {
                @Override
                public void onUsersMeRequestSuccess(ApiUser user) {
                    setUser(user);
                }
            }, at);
        }

        @Override
        protected void onSuccess(String response) {
            super.onSuccess(response);

            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                ArrayList<Row> rows = new ArrayList<>();
                parseRows(response, rows);
                Fragment fragment = DataSubFragment.newInstance(null, rows);
                MainActivity.this.addFragmentToBackStack(fragment, true);
            }
        }

        @Override
        protected void onComplete() {
            super.onComplete();

            if (mUser == null) {
                // looks like a bad access token, most likely a saved one
                ObjectAsFile.save(MainActivity.this, ObjectAsFile.ACCESS_TOKEN, null);
                startLoginActivity();
            }
        }
    }

    class UsersMeAvatarRequest extends Api.PostRequest {
        UsersMeAvatarRequest(ApiAccessToken at, String fileName, InputStream inputStream) {
            super(ApiConstants.URL_USERS_ME_AVATAR, new Api.Params(at));

            try {
                addFile(ApiConstants.URL_USERS_ME_AVATAR_PARAM_AVATAR, fileName, inputStream);
            } catch (IllegalAccessException e) {
                Log.e(getTag().toString(), e.toString());
            }
        }

        @Override
        protected void onSuccess(String response) {
            ApiBaseResponse data = App.getGsonInstance().fromJson(response, ApiBaseResponse.class);
            String error = data.getError();
            if (!TextUtils.isEmpty(error)) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onError(VolleyError error) {
            String message = getErrorMessage(error);

            if (message != null) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onComplete() {
            new UsersMeRequest(mAccessToken).start();
        }
    }

    static class IndexResponse {
        @SerializedName("links")
        Map<String, String> links;

        @SerializedName("post")
        Map<String, String> post;
    }
}
