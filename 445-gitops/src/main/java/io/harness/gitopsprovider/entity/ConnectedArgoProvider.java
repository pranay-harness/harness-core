package io.harness.gitopsprovider.entity;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.gitopsprovider.SearchTerm;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;

@Value
@Builder
@FieldNameConstants(innerTypeName = "GitOpsProviderKeys")
@EqualsAndHashCode(callSuper = true)
@Entity(value = "gitopsproviders", noClassnameStored = true)
@Persistent
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.GITOPS)
public class ConnectedArgoProvider extends GitOpsProvider {
  @NotBlank @SearchTerm String adapterUrl;

  @Override
  public GitOpsProviderType getGitOpsProviderType() {
    return GitOpsProviderType.CONNECTED_ARGO_PROVIDER;
  }
}
