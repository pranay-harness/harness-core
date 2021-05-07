package io.harness.ngtriggers.beans.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("ngTriggerType")
public enum WebhookTriggerType {
  @JsonProperty("Github") GITHUB,
  @JsonProperty("Gitlab") GITLAB,
  @JsonProperty("Bitbucket") BITBUCKET,
  @JsonProperty("Custom") CUSTOM,
  @JsonProperty("Aws CodeCommit") AWS_CODECOMMIT
}
