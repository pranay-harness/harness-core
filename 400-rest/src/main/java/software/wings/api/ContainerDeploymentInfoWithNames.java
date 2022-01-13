/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * This holds controllers info about containers.
 * @author rktummala on 08/24/17
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class ContainerDeploymentInfoWithNames extends BaseContainerDeploymentInfo {
  /**
   * In case of ECS, this would be a list of taskDefinitionArns.
   * In case of Kubernetes, this would be a list of controllerNames.
   */
  private String containerSvcName;
  private String namespace;
  // use this when containerSvcName is not unique as in case of ECS Daemon scheduling
  private String uniqueNameIdentifier;

  @Builder
  public ContainerDeploymentInfoWithNames(
      String clusterName, String containerSvcName, String namespace, String uniqueNameIdentifier) {
    super(clusterName);
    this.containerSvcName = containerSvcName;
    this.namespace = namespace;
    this.uniqueNameIdentifier = uniqueNameIdentifier;
  }
}
