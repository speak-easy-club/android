package com.speakeasy.Android.common;

import com.android.volley.VolleyError;
import com.google.gson.annotations.SerializedName;
import com.speakeasy.Android.App;
import com.speakeasy.Android.common.model.ApiAccessToken;
import com.speakeasy.Android.common.model.ApiUser;

public class ApiUsersMeRequest extends Api.GetRequest {
    private final Listener mListener;

    public ApiUsersMeRequest(Listener listener, ApiAccessToken at) {
        super(ApiConstants.URL_USERS_ME, new Api.Params(at));

        mListener = listener;
    }

    @Override
    protected void onSuccess(String response) {
        Response data = App.getGsonInstance().fromJson(response, Response.class);
        if (data.user == null) {
            onError(new VolleyError(data.getError()));
            return;
        }

        if (mListener != null) {
            mListener.onUsersMeRequestSuccess(data.user);
        }
    }

    public interface Listener {
        void onUsersMeRequestSuccess(ApiUser user);
    }

    private static class Response extends ApiBaseResponse {
        @SerializedName("user")
        ApiUser user;
    }
}