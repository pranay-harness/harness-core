package io.harness.ngtriggers.beans.source.webhook;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.awscodecommit.AwsCodeCommitSpec;
import io.harness.ngtriggers.beans.source.webhook.bitbucket.BitbucketSpec;
import io.harness.ngtriggers.beans.source.webhook.custom.CustomTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.github.GithubSpec;
import io.harness.ngtriggers.beans.source.webhook.gitlab.GitlabSpec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = GithubSpec.class, name = "Github")
  , @JsonSubTypes.Type(value = GitlabSpec.class, name = "Gitlab"),
      @JsonSubTypes.Type(value = BitbucketSpec.class, name = "Bitbucket"),
      @JsonSubTypes.Type(value = AwsCodeCommitSpec.class, name = "AWS CodeCommit"),
      @JsonSubTypes.Type(value = CustomTriggerSpec.class, name = "Custom")
})
@OwnedBy(PIPELINE)
public interface WebhookTriggerSpec_1 {}
