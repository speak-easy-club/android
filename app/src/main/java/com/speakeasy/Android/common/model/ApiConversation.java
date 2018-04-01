package com.speakeasy.Android.common.model;

import com.google.gson.annotations.SerializedName;
import com.speakeasy.Android.common.Api;
import com.speakeasy.Android.common.ApiConstants;

public class ApiConversation extends ApiDiscussion {

    public static ApiConversation incompleteWithId(final int id) {
        return new ApiConversation() {
            @Override
            public Integer getId() {
                return id;
            }

            @Override
            public boolean isComplete() {
                return false;
            }
        };
    }

    @SerializedName("conversation_id")
    Integer mId;

    @SerializedName("conversation_title")
    String mTitle;

    @SerializedName("creator_username")
    String mCreatorName;

    @SerializedName("conversation_create_date")
    Integer mCreateDate;

    @SerializedName("first_message")
    ApiConversationMessage mFirstMessage;

    @SerializedName("links")
    Links mLinks;

    @SerializedName("permissions")
    Permissions mPermissions;

    @Override
    public Integer getId() {
        return mId;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getCreatorName() {
        return mCreatorName;
    }

    @Override
    public String getCreatorAvatar() {
        return null;
    }

    @Override
    public Integer getCreateDate() {
        return mCreateDate;
    }

    @Override
    public ApiDiscussionMessage getFirstMessage() {
        return mFirstMessage;
    }

    @Override
    public String getPermalink() {
        if (mLinks == null) {
            return null;
        }

        return mLinks.mPermalink;
    }

    @Override
    public boolean canPostMessage() {
        if (mPermissions == null) {
            return false;
        }

        Boolean permission = mPermissions.mPostMessage;
        if (permission == null) {
            return false;
        }

        return permission;
    }

    @Override
    public boolean canUploadAttachment() {
        if (mPermissions == null) {
            return false;
        }

        Boolean permission = mPermissions.mUploadAttachment;
        if (permission == null) {
            return false;
        }

        return permission;
    }

    @Override
    public String getGetMessagesUrl() {
        return ApiConstants.URL_CONVERSATION_MESSAGES;
    }

    @Override
    public Api.Params getGetMessagesParams(int page, ApiAccessToken accessToken) {
        String fieldsInclude = null;
        if (page < 2) {
            fieldsInclude = "conversation";
        }

        return new Api.Params(accessToken)
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_CONVERSATION_ID, getId())
                .and(ApiConstants.PARAM_PAGE, page)
                .and(ApiConstants.PARAM_ORDER, ApiConstants.URL_CONVERSATION_MESSAGES_ORDER_REVERSE)
                .andFieldsInclude(ApiConversationMessage.class, fieldsInclude);
    }

    @Override
    public String getPostAttachmentsUrl(String attachmentHash, ApiAccessToken accessToken) {
        return Api.makeAttachmentsUrl(ApiConstants.URL_CONVERSATIONS_ATTACHMENTS, attachmentHash, accessToken);
    }

    @Override
    public String getPostMessagesUrl() {
        return ApiConstants.URL_CONVERSATION_MESSAGES;
    }

    @Override
    public Api.Params getPostMessagesParams(String bodyPlainText, String attachmentHash, ApiAccessToken accessToken) {
        return new Api.Params(accessToken)
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_CONVERSATION_ID, getId())
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_MESSAGE_BODY, bodyPlainText)
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_ATTACHMENT_HASH, attachmentHash)
                .and(ApiConstants.PARAM_FIELDS_INCLUDE, "message_id");
    }


    @SuppressWarnings("unused")
    static class Links extends ApiModel {
        @SerializedName("permalink")
        String mPermalink;
    }

    @SuppressWarnings("unused")
    static class Permissions extends ApiModel {
        @SerializedName("reply")
        Boolean mPostMessage;

        @SerializedName("upload_attachment")
        Boolean mUploadAttachment;
    }
}
