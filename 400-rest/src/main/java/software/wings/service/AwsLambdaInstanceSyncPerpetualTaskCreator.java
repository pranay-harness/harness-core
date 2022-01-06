/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient.FUNCTION_NAME;
import static io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient.QUALIFIER;
import static io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClient.START_DATE;

import static software.wings.service.InstanceSyncConstants.HARNESS_APPLICATION_ID;
import static software.wings.service.InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;

import static java.lang.Long.parseLong;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskClientParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;

import software.wings.api.DeploymentSummary;
import software.wings.api.lambda.AwsLambdaDeploymentInfo;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.service.intfc.instance.ServerlessInstanceService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(CDP)
@TargetModule(HarnessModule._360_CG_MANAGER)
public class AwsLambdaInstanceSyncPerpetualTaskCreator extends AbstractInstanceSyncPerpetualTaskCreator {
  @Inject ServerlessInstanceService serverlessInstanceService;

  @Override
  public List<String> createPerpetualTasks(InfrastructureMapping infrastructureMapping) {
    Set<AwsLambdaFunction> lambdaFunctions = getActiveLambdaFunctions(infrastructureMapping);
    return createPerpetualTasksFrom(lambdaFunctions, infrastructureMapping);
  }

  @Override
  public List<String> createPerpetualTasksForNewDeployment(List<DeploymentSummary> deploymentSummaries,
      List<PerpetualTaskRecord> existingPerpetualTasks, InfrastructureMapping infrastructureMapping) {
    final Set<AwsLambdaFunction> existingLambdaFunctions = extractLambdaFunctionsFromRecord(existingPerpetualTasks);
    final Set<AwsLambdaFunction> lambdaFunctionsFromNewDeployment =
        extractLambdaFunctionsFromNewDeployment(deploymentSummaries);

    SetView<AwsLambdaFunction> eligibleLambdaFunctions =
        Sets.difference(lambdaFunctionsFromNewDeployment, existingLambdaFunctions);

    return createPerpetualTasksFrom(eligibleLambdaFunctions, infrastructureMapping);
  }

  private Set<AwsLambdaFunction> extractLambdaFunctionsFromNewDeployment(List<DeploymentSummary> deploymentSummaries) {
    return emptyIfNull(deploymentSummaries)
        .stream()
        .map(deploymentSummary
            -> Pair.of(
                deploymentSummary.getDeployedAt(), (AwsLambdaDeploymentInfo) deploymentSummary.getDeploymentInfo()))
        .map(deploymentInfoWithDate
            -> AwsLambdaFunction.builder()
                   .functionName(deploymentInfoWithDate.getValue().getFunctionName())
                   .functionVersion(deploymentInfoWithDate.getValue().getVersion())
                   .startDate(deploymentInfoWithDate.getKey())
                   .build())
        .collect(Collectors.toSet());
  }

  private Set<AwsLambdaFunction> extractLambdaFunctionsFromRecord(List<PerpetualTaskRecord> existingPerpetualTasks) {
    return emptyIfNull(existingPerpetualTasks)
        .stream()
        .map(record
            -> AwsLambdaFunction.builder()
                   .functionName(record.getClientContext().getClientParams().get(FUNCTION_NAME))
                   .functionVersion(record.getClientContext().getClientParams().get(QUALIFIER))
                   .startDate(parseLong(record.getClientContext().getClientParams().get(START_DATE)))
                   .build())
        .collect(Collectors.toSet());
  }

  private List<String> createPerpetualTasksFrom(
      Set<AwsLambdaFunction> lambdaFunctions, InfrastructureMapping infrastructureMapping) {
    return lambdaFunctions.stream()
        .map(lambdaFunction
            -> AwsLambdaInstanceSyncPerpetualTaskClientParams.builder()
                   .appId(infrastructureMapping.getAppId())
                   .inframappingId(infrastructureMapping.getUuid())
                   .functionName(lambdaFunction.getFunctionName())
                   .qualifier(lambdaFunction.getFunctionVersion())
                   .startDate(String.valueOf(lambdaFunction.getStartDate()))
                   .build())
        .map(clientParams -> create(clientParams, infrastructureMapping))
        .collect(Collectors.toList());
  }

  private String create(
      AwsLambdaInstanceSyncPerpetualTaskClientParams clientParams, InfrastructureMapping infraMapping) {
    Map<String, String> paramMap = ImmutableMap.<String, String>builder()
                                       .put(HARNESS_APPLICATION_ID, clientParams.getAppId())
                                       .put(INFRASTRUCTURE_MAPPING_ID, clientParams.getInframappingId())
                                       .put(FUNCTION_NAME, clientParams.getFunctionName())
                                       .put(QUALIFIER, clientParams.getQualifier())
                                       .put(START_DATE, clientParams.getStartDate())
                                       .build();

    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder().clientParams(paramMap).build();

    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                                         .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                                         .build();

    return perpetualTaskService.createTask(PerpetualTaskType.AWS_LAMBDA_INSTANCE_SYNC, infraMapping.getAccountId(),
        clientContext, schedule, false, getTaskDescription(infraMapping));
  }

  private Set<AwsLambdaFunction> getActiveLambdaFunctions(InfrastructureMapping infrastructureMapping) {
    return emptyIfNull(getActiveServerlessInstances(infrastructureMapping))
        .stream()
        .map(serverlessInstance
            -> AwsLambdaFunction.builder()
                   .functionName(serverlessInstance.getLambdaInstanceKey().getFunctionName())
                   .functionVersion(serverlessInstance.getLambdaInstanceKey().getFunctionVersion())
                   .startDate(serverlessInstance.getLastDeployedAt())
                   .build())
        .collect(Collectors.toSet());
  }

  private List<ServerlessInstance> getActiveServerlessInstances(InfrastructureMapping infrastructureMapping) {
    return serverlessInstanceService.list(infrastructureMapping.getUuid(), infrastructureMapping.getAppId());
  }

  @Data
  @Builder
  static class AwsLambdaFunction {
    String functionName;
    String functionVersion;
    long startDate;
  }
}
