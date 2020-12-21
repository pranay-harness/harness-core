package io.harness.delegate.beans.connector.scm.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GitlabHttpAuthenticationType {
  @JsonProperty(GitlabConnectorConstants.USERNAME_AND_PASSWORD)
  USERNAME_AND_PASSWORD(GitlabConnectorConstants.USERNAME_AND_PASSWORD),
  @JsonProperty(GitlabConnectorConstants.USERNAME_AND_TOKEN)
  USERNAME_AND_TOKEN(GitlabConnectorConstants.USERNAME_AND_TOKEN);

  private final String displayName;

  GitlabHttpAuthenticationType(String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return displayName;
  }
}
