package io.harness.ngtriggers.beans.source.webhook;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubTriggerSpec.class, name = "GITHUB")
  , @JsonSubTypes.Type(value = GitlabTriggerSpec.class, name = "GITLAB"),
      @JsonSubTypes.Type(value = BitbucketTriggerSpec.class, name = "BITBUCKET"),
      @JsonSubTypes.Type(value = AwsCodeCommitTriggerSpec.class, name = "AWS_CODECOMMIT"),
      @JsonSubTypes.Type(value = CustomWebhookTriggerSpec.class, name = "CUSTOM")
})
@JsonDeserialize()
public interface WebhookTriggerSpec {
  RepoSpec getRepoSpec();
  WebhookEvent getEvent();
  List<WebhookAction> getActions();
  List<WebhookCondition> getPayloadConditions();
  List<String> getPathFilters();
  WebhookSourceRepo getType();

  default List<WebhookCondition> getHeaderConditions() {
    return null;
  }
}
