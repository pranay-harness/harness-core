package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitConfigDTO implements ConnectorConfigDTO {
  @JsonProperty("type") GitAuthType gitAuthType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = GitHTTPAuthenticationDTO.class, name = "Http")
    , @JsonSubTypes.Type(value = GitSSHAuthenticationDTO.class, name = "Ssh")
  })
  GitAuthenticationDTO gitAuth;

  @JsonProperty("gitSync") GitSyncConfig gitSyncConfig;

  @Builder
  public GitConfigDTO(GitAuthType gitAuthType, GitAuthenticationDTO gitAuth, GitSyncConfig gitSyncConfig) {
    this.gitAuthType = gitAuthType;
    this.gitAuth = gitAuth;
    this.gitSyncConfig = gitSyncConfig;
  }
}
