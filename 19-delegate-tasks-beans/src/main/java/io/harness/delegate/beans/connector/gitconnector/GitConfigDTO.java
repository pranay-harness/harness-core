package io.harness.delegate.beans.connector.gitconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitConfigDTO extends ConnectorConfigDTO {
  @NotNull @JsonProperty("type") GitAuthType gitAuthType;
  @NotNull @JsonProperty("connectionType") GitConnectionType gitConnectionType;
  @NotNull String url;
  String branchName;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  GitAuthenticationDTO gitAuth;

  @JsonProperty("gitSync") GitSyncConfig gitSyncConfig;

  @Builder
  public GitConfigDTO(GitAuthType gitAuthType, GitAuthenticationDTO gitAuth, GitSyncConfig gitSyncConfig,
      GitConnectionType gitConnectionType, String url, String branchName) {
    this.gitAuthType = gitAuthType;
    this.gitAuth = gitAuth;
    this.gitSyncConfig = gitSyncConfig;
    this.gitConnectionType = gitConnectionType;
    this.url = url;
    this.branchName = branchName;
  }
}
