package io.harness.delegate.beans.connector.scm.bitbucket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BitbucketApiAccessType {
  @JsonProperty(BitbucketConnectorConstants.USERNAME_AND_TOKEN)
  USERNAME_AND_TOKEN(BitbucketConnectorConstants.USERNAME_AND_TOKEN);

  private final String displayName;

  BitbucketApiAccessType(String displayName) {
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
