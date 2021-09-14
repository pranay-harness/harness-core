/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.gitops;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.GITOPS)
public class ConnectedArgoGitOpsInfoDTO extends GitOpsInfoDTO {
  @NotNull private String adapterUrl;

  @Override
  public GitOpsProviderType getGitProviderType() {
    return GitOpsProviderType.CONNECTED_ARGO_PROVIDER;
  }
}
