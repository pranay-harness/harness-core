package io.harness.delegate.beans.connector.gitops;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.GITOPS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = ConnectedArgoGitOpsInfoDTO.class, name = "CONNECTED_ARGO_PROVIDER")
  , @JsonSubTypes.Type(value = ManagedArgoGitOpsInfoDTO.class, name = "MANAGED_ARGO_PROVIDER")
})
public abstract class GitOpsInfoDTO {
  @JsonProperty("type") public abstract GitOpsProviderType getGitProviderType();
}
