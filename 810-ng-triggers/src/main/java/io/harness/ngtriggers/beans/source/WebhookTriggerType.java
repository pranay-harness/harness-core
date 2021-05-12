package io.harness.ngtriggers.beans.source;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.TypeAlias;

@TypeAlias("ngTriggerType")
@OwnedBy(PIPELINE)
public enum WebhookTriggerType {
  @JsonProperty("Github") GITHUB("Github"),
  @JsonProperty("Gitlab") GITLAB("Gitlab"),
  @JsonProperty("Bitbucket") BITBUCKET("Bitbucket"),
  @JsonProperty("Custom") CUSTOM("Custom"),
  @JsonProperty("Aws CodeCommit") AWS_CODECOMMIT("Aws CodeCommit");

  private String value;

  WebhookTriggerType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
