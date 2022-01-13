/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
