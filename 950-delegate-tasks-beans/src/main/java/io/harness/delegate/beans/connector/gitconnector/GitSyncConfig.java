package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSyncConfig {
  @JsonProperty("enabled") boolean isSyncEnabled;
  CustomCommitAttributes customCommitAttributes;
}
