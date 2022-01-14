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
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@Entity(value = "gitopsproviders", noClassnameStored = true)
@Persistent
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.GITOPS)
public class ManagedGitOpsProvider extends GitOpsProvider {
  @NotBlank @SearchTerm String namespace;

  public ManagedGitOpsProvider(String namespace) {
    this.namespace = namespace;
    this.type = GitOpsProviderType.MANAGED_ARGO_PROVIDER;
  }

  @Override
  public GitOpsProviderType getGitOpsProviderType() {
    return GitOpsProviderType.MANAGED_ARGO_PROVIDER;
  }
}
