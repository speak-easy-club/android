package com.speakeasy.Android.gcm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.speakeasy.Android.common.Api;
import com.speakeasy.Android.App;
import com.speakeasy.Android.BuildConfig;
import com.speakeasy.Android.R;
import com.speakeasy.Android.common.model.ApiAccessToken;

import java.util.Locale;

public class RegistrationService extends IntentService {

    public static final String EXTRA_ACCESS_TOKEN = "access_token";
    public static final String EXTRA_UNREGISTER = "unregister";
    public static final String ACTION_REGISTRATION = "com.speakeasy.api.androiddemo.gcm.REGISTRATION";
    public static final String ACTION_REGISTRATION_UNREGISTERED = "unregistered";

    private static final String TAG = "RegistrationService";
    private static long mLastUserId = -1;

    public RegistrationService() {
        super(TAG);
    }

    public static boolean canRun(Context context) {
        boolean canRun;

        // check for push server address
        canRun = !TextUtils.isEmpty(BuildConfig.PUSH_SERVER);

        // check for Google Play Services
        GoogleApiAvailability gaa = GoogleApiAvailability.getInstance();
        int resultCode = gaa.isGooglePlayServicesAvailable(context);
        canRun = canRun && (resultCode == ConnectionResult.SUCCESS);

        return canRun;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            synchronized (TAG) {
                InstanceID instanceID = InstanceID.getInstance(this);
                String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                        GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                if (BuildConfig.DEBUG) {
                    Log.v(TAG, "GCM token=" + token);
                }

                if (intent.getBooleanExtra(EXTRA_UNREGISTER, false)) {
                    sendUnregistrationToServer(token);
                } else {
                    ApiAccessToken at = null;
                    if (intent.hasExtra(EXTRA_ACCESS_TOKEN)) {
                        at = (ApiAccessToken) intent.getSerializableExtra(EXTRA_ACCESS_TOKEN);
                    }

                    sendRegistrationToServer(token, at);
                }
            }

        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Unable to get GCM token", e);
            }
        }
    }

    private void sendRegistrationToServer(String gcmToken, ApiAccessToken at) {
        long userId = 0;

        if (at != null) {
            userId = at.getUserId();
        }

        if (mLastUserId == -1 || mLastUserId != userId) {
            new RegisterRequest(gcmToken, userId, at).start();
            mLastUserId = userId;
        }
    }

    private void sendUnregistrationToServer(String gcmToken) {
            new UnregisterRequest(gcmToken).start();
            mLastUserId = -1;
    }

    private static class RegisterRequest extends Api.PushServerRequest {
        private final long mUserId;

        RegisterRequest(String gcmToken, long userId, ApiAccessToken at) {
            super(gcmToken, userId > 0 ? String.format(Locale.US, "user_notification_%d", userId) : "", at);

            mUserId = userId;
        }

        @Override
        protected void onSuccess(String response) {
            final Toast t;
            final Context c = App.getInstance().getApplicationContext();

            if (mUserId == 0) {
                t = Toast.makeText(c, R.string.gcm_register_success, Toast.LENGTH_LONG);
            } else {
                t = Toast.makeText(
                        c,
                        String.format(c.getString(R.string.gcm_subscribe_x_success), String.valueOf(mUserId)),
                        Toast.LENGTH_LONG
                );
            }

            t.show();

            Intent broadcastIntent = new Intent(ACTION_REGISTRATION);
            c.sendBroadcast(broadcastIntent);
        }
    }

    private static class UnregisterRequest extends Api.PostRequest {
        UnregisterRequest(String gcmToken) {
            super(
                    BuildConfig.PUSH_SERVER + "/unregister",
                    new Api.Params("device_type", "android")
                            .and("device_id", gcmToken)
                            .and("oauth_client_id", BuildConfig.CLIENT_ID)
            );
        }

        @Override
        protected void onSuccess(String response) {
            final Context c = App.getInstance().getApplicationContext();
            Toast.makeText(c, R.string.gcm_unregister_success, Toast.LENGTH_LONG).show();

            Intent broadcastIntent = new Intent(ACTION_REGISTRATION);
            broadcastIntent.putExtra(ACTION_REGISTRATION_UNREGISTERED, true);
            c.sendBroadcast(broadcastIntent);
        }
    }

}