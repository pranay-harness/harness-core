/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SweepingOutput;

import software.wings.beans.GitFetchFilesConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("K8sGitConfigMapInfo")
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class K8sGitConfigMapInfo implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME_PREFIX = "k8sGitConfigMapInfo";

  private Map<String, GitFetchFilesConfig> gitFetchFilesConfigMap;
  private String serviceId;

  @Override
  public String getType() {
    return "k8sGitConfigMapInfo";
  }
}
