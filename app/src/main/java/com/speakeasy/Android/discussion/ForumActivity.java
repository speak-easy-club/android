package com.speakeasy.Android.discussion;

import com.google.gson.annotations.SerializedName;
import com.speakeasy.Android.App;
import com.speakeasy.Android.common.Api;
import com.speakeasy.Android.common.ApiBaseResponse;
import com.speakeasy.Android.common.ApiConstants;
import com.speakeasy.Android.common.model.ApiAccessToken;
import com.speakeasy.Android.common.model.ApiDiscussion;
import com.speakeasy.Android.common.model.ApiThread;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForumActivity extends DiscussionListActivity {

    private static final Pattern patternUrl = Pattern.compile("(index\\.php\\?|/)forums/(\\d+)/");

    public static int getForumIdFromUrl(String url) {
        Matcher m = patternUrl.matcher(url);
        if (m.find()) {
            String forumId = m.group(2);
            return Integer.parseInt(forumId);
        }

        return 0;
    }

    @Override
    String getGetDiscussionsUrl() {
        return ApiConstants.URL_THREADS;
    }

    @Override
    Api.Params getGetDiscussionsParams(int page, ApiAccessToken accessToken) {
        String fieldsInclude = null;
        if (page < 2) {
            fieldsInclude = "forum";
        }

        return new Api.Params(accessToken)
                .and(ApiConstants.PARAM_PAGE, page)
                .and(ApiConstants.URL_THREADS_PARAM_FORUM_ID, mDiscussionContainerId)
                .andFieldsInclude(ApiThread.class, fieldsInclude);
    }

    @Override
    ParsedDiscussions parseResponseForDiscussions(String response) {
        return App.getGsonInstance().fromJson(response, ThreadsResponse.class);
    }

    static class ThreadsResponse extends ApiBaseResponse implements ParsedDiscussions {
        @SerializedName("threads")
        List<ApiThread> threads;

        @Override
        public List<? extends ApiDiscussion> getDiscussions() {
            return threads;
        }

        @Override
        public Integer getPage() {
            Links links = getLinks();
            if (links == null) {
                return null;
            }

            return links.getPage();
        }

        @Override
        public Integer getPages() {
            Links links = getLinks();
            if (links == null) {
                return null;
            }

            return links.getPages();
        }
    }
}
