package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum GitConnectionType {
  @JsonProperty(GitConfigConstants.ACCOUNT) ACCOUNT,
  @JsonProperty(GitConfigConstants.REPO) REPO
}
