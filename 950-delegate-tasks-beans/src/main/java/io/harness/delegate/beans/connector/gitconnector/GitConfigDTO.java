package io.harness.delegate.beans.connector.gitconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ExecutionCapabilityDemanderWithScope;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Collections;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitConfigDTO extends ConnectorConfigDTO implements ExecutionCapabilityDemanderWithScope {
  @NotNull @JsonProperty("type") GitAuthType gitAuthType;
  @NotNull @JsonProperty("connectionType") GitConnectionType gitConnectionType;
  @NotNull String url;
  String branchName;

  @JsonProperty("spec")
  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @JsonSubTypes({
    @JsonSubTypes.Type(value = GitHTTPAuthenticationDTO.class, name = GitConfigConstants.HTTP)
    , @JsonSubTypes.Type(value = GitSSHAuthenticationDTO.class, name = GitConfigConstants.SSH)
  })
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

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return gitAuth;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // todo @deepak: This function is yet to be implemented, the git capability requires the
    // encryption details to be sent, to get encryption data details in 19-delegate-tasks, we
    // will need refractoring
    return null;
  }
}
