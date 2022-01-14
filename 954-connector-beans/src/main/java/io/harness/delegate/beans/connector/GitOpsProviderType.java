package io.harness.delegate.beans.connector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.GITOPS)
public enum GitOpsProviderType {
  CONNECTED_ARGO_PROVIDER,
  MANAGED_ARGO_PROVIDER;
}
