package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonTypeName("Git")
public class GitConfigDTO extends ConnectorConfigDTO {
  @JsonProperty("type") GitAuthType gitAuthType;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @NotNull
  GitAuthenticationDTO gitAuth;

  @JsonProperty("gitSync") GitSyncConfig gitSyncConfig;

  @Builder
  public GitConfigDTO(GitAuthType gitAuthType, GitAuthenticationDTO gitAuth, GitSyncConfig gitSyncConfig) {
    this.gitAuthType = gitAuthType;
    this.gitAuth = gitAuth;
    this.gitSyncConfig = gitSyncConfig;
  }
}
