package com.speakeasy.Android.discussion;

import com.google.gson.annotations.SerializedName;
import com.speakeasy.Android.App;
import com.speakeasy.Android.common.ApiBaseResponse;
import com.speakeasy.Android.common.model.ApiDiscussion;
import com.speakeasy.Android.common.model.ApiDiscussionMessage;
import com.speakeasy.Android.common.model.ApiPost;
import com.speakeasy.Android.common.model.ApiThread;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadActivity extends DiscussionActivity {

    private static final Pattern patternUrl = Pattern.compile("(index\\.php\\?|/)threads/(\\d+)/");

    public static int getThreadIdFromUrl(String url) {
        Matcher m = patternUrl.matcher(url);
        if (m.find()) {
            String threadId = m.group(2);
            return Integer.parseInt(threadId);
        }

        return 0;
    }

    @Override
    void setDiscussion(int discussionId) {
        setDiscussion(ApiThread.incompleteWithId(discussionId));
    }

    @Override
    ParsedMessages parseResponseForMessages(String response) {
        return App.getGsonInstance().fromJson(response, PostsResponse.class);
    }

    static class PostsResponse extends ApiBaseResponse implements ParsedMessages {
        @SerializedName("posts")
        List<ApiPost> posts;

        @SerializedName("thread")
        ApiThread thread;

        @Override
        public List<? extends ApiDiscussionMessage> getMessages() {
            return posts;
        }

        @Override
        public ApiDiscussion getDiscussion() {
            return thread;
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
