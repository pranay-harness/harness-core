/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api.ecs;

import io.harness.delegate.beans.pcf.ResizeStrategy;

import software.wings.beans.AwsElbConfig;
import software.wings.beans.TaskType;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.yaml.GitFetchFilesFromMultipleRepoResult;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.sm.states.EcsSetUpDataBag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
public class EcsBGSetupStateExecutionData extends EcsSetupStateExecutionData {
  private String prodListenerArn;
  private String stageListenerArn;
  private String stageListenerRuleArn;
  private String prodListenerRuleArn;
  private boolean isUseSpecificListenerRuleArn;
  private String stageListenerPort;

  @Builder(builderMethodName = "ecsBGStateExecutionDataBuilder")
  private EcsBGSetupStateExecutionData(String activityId, String accountId, String appId, String commandName,
      TaskType taskType, Map<K8sValuesLocation, ApplicationManifest> applicationManifestMap,
      EcsSetUpDataBag ecsSetUpDataBag, String roleArn, String targetPort, String maxInstances, String fixedInstances,
      String ecsServiceName, String targetGroupArn, boolean useLoadBalancer, String loadBalancerName,
      String targetContainerName, String desiredInstanceCount, int serviceSteadyStateTimeout,
      ResizeStrategy resizeStrategy, List<AwsAutoScalarConfig> awsAutoScalarConfigs,
      GitFetchFilesFromMultipleRepoResult fetchFilesResult, String prodListenerArn, String stageListenerArn,
      String stageListenerRuleArn, String prodListenerRuleArn, boolean isUseSpecificListenerRuleArn,
      String stageListenerPort) {
    super(activityId, accountId, appId, commandName, taskType, applicationManifestMap, ecsSetUpDataBag, roleArn,
        targetPort, maxInstances, fixedInstances, ecsServiceName, targetGroupArn, useLoadBalancer, loadBalancerName,
        targetContainerName, desiredInstanceCount, serviceSteadyStateTimeout, resizeStrategy, awsAutoScalarConfigs,
        new ArrayList<AwsElbConfig>(), false, fetchFilesResult);
    this.prodListenerArn = prodListenerArn;
    this.stageListenerArn = stageListenerArn;
    this.stageListenerRuleArn = stageListenerRuleArn;
    this.prodListenerRuleArn = prodListenerRuleArn;
    this.isUseSpecificListenerRuleArn = isUseSpecificListenerRuleArn;
    this.stageListenerPort = stageListenerPort;
  }
}
