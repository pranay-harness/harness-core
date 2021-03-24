package io.harness.constants;

public interface Constants {
  String X_GIT_HUB_EVENT = "X-GitHub-Event";
  String X_GIT_LAB_EVENT = "X-Gitlab-Event";
  String X_BIT_BUCKET_EVENT = "X-Event-Key";
  String BITBUCKET_SERVER_HEADER_KEY = "X-Request-Id";
  String BITBUCKET_CLOUD_HEADER_KEY = "X-Request-UUID";
  String X_AMZ_SNS_MESSAGE_TYPE = "X-Amz-Sns-Message-Type";
  String AMZ_SUBSCRIPTION_CONFIRMATION_TYPE = "SubscriptionConfirmation";
  String X_HARNESS_WEBHOOK_TOKEN = "X-HARNESS-WEBHOOK-TOKEN";
  String X_HARNESS_TRIGGER_ID = "X-HARNESS-TRIGGER-ID";
  String UNRECOGNIZED_WEBHOOK = "Unrecognized Webhook";
}
