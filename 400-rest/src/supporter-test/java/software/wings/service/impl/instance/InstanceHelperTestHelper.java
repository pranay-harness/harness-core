/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.task.azure.response.AzureVMInstanceData;

import software.wings.api.AmiStepExecutionSummary;
import software.wings.api.CommandStepExecutionSummary;
import software.wings.api.ContainerServiceData;
import software.wings.api.HelmSetupExecutionSummary;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseExecutionData.PhaseExecutionDataBuilder;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.CodeDeployParams;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.StepExecutionSummary.StepExecutionSummaryBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@OwnedBy(DX)
public class InstanceHelperTestHelper {
  public static final String INFRA_MAP_ID = "infraMap_1";
  public static final String CODE_DEPLOY_DEPLOYMENT_ID = "codeDeployment_id";
  public static final String CODE_DEPLOY_APP_NAME = "codeDeployment_app";
  public static final String CODE_DEPLOY_GROUP_NAME = "codeDeployment_group";
  public static final String CODE_DEPLOY_KEY = "codeDeployment_key";
  public static final String APP_ID = "app_1";
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String SERVICE_ID = "SERVICE_ID";
  public static final String WORKFLOW_EXECUTION_ID = "workflow_1";
  public static final String CLUSTER_NAME = "clusterName";
  public static final String COMPUTE_PROVIDER_ID = "computeProvider_1";

  private List<InstanceStatusSummary> getInstanceStatusSummariesForPDC() {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder()
                                                                       .uuid("host_1")
                                                                       .hostName("hostName1")
                                                                       .instanceId("instance1")
                                                                       .publicDns("instance1")
                                                                       .build())
                                                             .uuid("instance_1")
                                                             .build())
                                    .build());

    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder()
                                                                       .uuid("host_2")
                                                                       .hostName("hostName2")
                                                                       .instanceId("instance2")
                                                                       .publicDns("instance2")
                                                                       .build())
                                                             .uuid("instance_2")
                                                             .build())
                                    .build());

    return instanceStatusSummaries;
  }

  private List<InstanceStatusSummary> getInstanceStatusSummariesForAws(
      com.amazonaws.services.ec2.model.Instance instance1, com.amazonaws.services.ec2.model.Instance instance2) {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder().ec2Instance(instance1).build())
                                                             .uuid("instance_1")
                                                             .build())
                                    .build());

    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder().ec2Instance(instance2).build())
                                                             .uuid("instance_2")
                                                             .build())
                                    .build());

    return instanceStatusSummaries;
  }

  private List<InstanceStatusSummary> getInstanceStatusSummariesForAzure(
      AzureVMInstanceData azureInstance1, AzureVMInstanceData azureInstance2) {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(
        InstanceStatusSummaryBuilder.anInstanceStatusSummary()
            .withStatus(ExecutionStatus.SUCCESS)
            .withInstanceElement(
                InstanceElement.Builder.anInstanceElement()
                    .host(HostElement.builder().azureVMInstance(azureInstance1).uuid("instance_1").build())
                    .uuid("instance_1")
                    .build())
            .build());

    instanceStatusSummaries.add(
        InstanceStatusSummaryBuilder.anInstanceStatusSummary()
            .withStatus(ExecutionStatus.SUCCESS)
            .withInstanceElement(
                InstanceElement.Builder.anInstanceElement()
                    .host(HostElement.builder().azureVMInstance(azureInstance2).uuid("instance_2").build())
                    .uuid("instance_2")
                    .build())
            .build());

    return instanceStatusSummaries;
  }

  private List<InstanceStatusSummary> getInstanceStatusSummariesForGCP() {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder().build())
                                                             .uuid("instance_1")
                                                             .build())
                                    .build());

    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder().build())
                                                             .uuid("instance_2")
                                                             .build())
                                    .build());

    return instanceStatusSummaries;
  }

  private List<InstanceStatusSummary> getInstanceStatusSummariesForECS(
      com.amazonaws.services.ec2.model.Instance instance1, com.amazonaws.services.ec2.model.Instance instance2) {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder().ec2Instance(instance1).build())
                                                             .uuid("instance_1")
                                                             .build())
                                    .build());

    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .host(HostElement.builder().ec2Instance(instance2).build())
                                                             .uuid("instance_2")
                                                             .build())
                                    .build());

    return instanceStatusSummaries;
  }

  public PhaseExecutionData initExecutionSummary(com.amazonaws.services.ec2.model.Instance instance1,
      com.amazonaws.services.ec2.model.Instance instance2, AzureVMInstanceData azureInstance1,
      AzureVMInstanceData azureInstance2, InfrastructureMappingType infrastructureMappingType, long endTime,
      String deploymentType) {
    List<InstanceStatusSummary> instanceStatusSummaries = null;

    if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH == infrastructureMappingType) {
      instanceStatusSummaries = getInstanceStatusSummariesForPDC();
    } else if (InfrastructureMappingType.AWS_SSH == infrastructureMappingType) {
      instanceStatusSummaries = getInstanceStatusSummariesForAws(instance1, instance2);
    } else if (InfrastructureMappingType.AWS_AMI == infrastructureMappingType) {
      instanceStatusSummaries = getInstanceStatusSummariesForAws(instance1, instance2);
    } else if (InfrastructureMappingType.AWS_AWS_CODEDEPLOY == infrastructureMappingType) {
      instanceStatusSummaries = getInstanceStatusSummariesForAws(instance1, instance2);
    } else if (InfrastructureMappingType.AWS_ECS == infrastructureMappingType) {
      instanceStatusSummaries = getInstanceStatusSummariesForAws(instance1, instance2);
    } else if (InfrastructureMappingType.AZURE_INFRA == infrastructureMappingType) {
      instanceStatusSummaries = getInstanceStatusSummariesForAzure(azureInstance1, azureInstance2);
    }

    return initExecutionSummary(instanceStatusSummaries, endTime, deploymentType);
  }

  public PhaseExecutionData initKubernetesExecutionSummary(
      InfrastructureMappingType infrastructureMappingType, long endTime, String deploymentType, boolean helm) {
    List<InstanceStatusSummary> instanceStatusSummaries = null;
    if (InfrastructureMappingType.GCP_KUBERNETES == infrastructureMappingType) {
      instanceStatusSummaries = getInstanceStatusSummariesForGCP();
    }
    return initExecutionSummary(instanceStatusSummaries, endTime, deploymentType);
  }

  private PhaseExecutionData initExecutionSummary(
      List<InstanceStatusSummary> instanceStatusSummaries, long endTime, String deploymentType) {
    ElementExecutionSummary summary = ElementExecutionSummaryBuilder.anElementExecutionSummary()
                                          .withInstanceStatusSummaries(instanceStatusSummaries)
                                          .withStatus(ExecutionStatus.SUCCESS)
                                          .build();

    return PhaseExecutionDataBuilder.aPhaseExecutionData()
        .withServiceId(SERVICE_ID)
        .withServiceName("serviceName")
        .withElementStatusSummary(Lists.newArrayList(summary))
        .withComputeProviderId("computeProvider_1")
        .withComputeProviderName("computeProviderName")
        .withInfraMappingId(INFRA_MAP_ID)
        .withEndTs(endTime)
        .withDeploymentType(deploymentType)
        .build();
  }

  public void assertInstances(List instances, InstanceType instanceType, InfrastructureMappingType mappingType,
      HostInstanceKey hostInstanceKey1, HostInstanceKey hostInstanceKey2, InstanceInfo instanceInfo1,
      InstanceInfo instanceInfo2, long endsAtTime) {
    assertThat(instances).isNotNull().hasSize(2).containsExactlyInAnyOrder(
        Instance.builder()
            .instanceType(instanceType)
            .hostInstanceKey(hostInstanceKey1)
            .envId("env_1")
            .envName("envName")
            .envType(EnvironmentType.PROD)
            .accountId(ACCOUNT_ID)
            .serviceId(SERVICE_ID)
            .serviceName("serviceName")
            .appName("app1")
            .infraMappingId(INFRA_MAP_ID)
            .infraMappingType(
                (mappingType == InfrastructureMappingType.AZURE_INFRA) ? mappingType.name() : mappingType.getName())
            .computeProviderId("computeProvider_1")
            .computeProviderName("computeProviderName")
            .lastArtifactStreamId("artifactStream_1")
            .lastArtifactId("artifact_1")
            .lastArtifactName("artifact1")
            .lastArtifactSourceName("sourceName")
            .lastArtifactBuildNum("1.0")
            .lastDeployedById("user_1")
            .lastDeployedByName("user1")
            .lastDeployedAt(endsAtTime)
            .lastWorkflowExecutionId(WORKFLOW_EXECUTION_ID)
            .lastWorkflowExecutionName("workflow1")
            .appId(APP_ID)
            .createdAt(0)
            .lastUpdatedAt(0)
            .instanceInfo(instanceInfo1)
            .build(),
        Instance.builder()
            .instanceType(instanceType)
            .hostInstanceKey(hostInstanceKey2)
            .envId("env_1")
            .envName("envName")
            .envType(EnvironmentType.PROD)
            .accountId(ACCOUNT_ID)
            .serviceId(SERVICE_ID)
            .serviceName("serviceName")
            .appName("app1")
            .infraMappingId(INFRA_MAP_ID)
            .infraMappingType(
                (mappingType == InfrastructureMappingType.AZURE_INFRA) ? mappingType.name() : mappingType.getName())
            .computeProviderId("computeProvider_1")
            .computeProviderName("computeProviderName")
            .lastArtifactStreamId("artifactStream_1")
            .lastArtifactId("artifact_1")
            .lastArtifactName("artifact1")
            .lastArtifactSourceName("sourceName")
            .lastArtifactBuildNum("1.0")
            .lastDeployedById("user_1")
            .lastDeployedByName("user1")
            .lastDeployedAt(endsAtTime)
            .lastWorkflowExecutionId(WORKFLOW_EXECUTION_ID)
            .lastWorkflowExecutionName("workflow1")
            .appId(APP_ID)
            .createdAt(0)
            .lastUpdatedAt(0)
            .instanceInfo(instanceInfo2)
            .build());
  }

  public PhaseExecutionSummary initPhaseExecutionSummary(
      InfrastructureMappingType infrastructureMappingType, String phaseStepExecutionSummaryString) {
    List<StepExecutionSummary> stepExecutionSummaries = null;

    if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH == infrastructureMappingType) {
      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build());

    } else if (InfrastructureMappingType.AWS_SSH == infrastructureMappingType) {
      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build());

    } else if (InfrastructureMappingType.AZURE_INFRA == infrastructureMappingType) {
      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build());

    } else if (InfrastructureMappingType.AWS_AMI == infrastructureMappingType) {
      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              AmiStepExecutionSummary.builder()
                  .instanceCount(1)
                  .instanceUnitType(InstanceUnitType.COUNT)
                  .newInstanceData(
                      asList(ContainerServiceData.builder().desiredCount(1).name("asgNew").previousCount(1).build()))
                  .oldInstanceData(
                      asList(ContainerServiceData.builder().desiredCount(1).name("asgOld").previousCount(1).build()))
                  .build());

    } else if (InfrastructureMappingType.AWS_AWS_CODEDEPLOY == infrastructureMappingType) {
      CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
      commandStepExecutionSummary.setCodeDeployDeploymentId(CODE_DEPLOY_DEPLOYMENT_ID);
      commandStepExecutionSummary.setCodeDeployParams(CodeDeployParams.builder()
                                                          .applicationName(CODE_DEPLOY_APP_NAME)
                                                          .deploymentGroupName(CODE_DEPLOY_GROUP_NAME)
                                                          .key(CODE_DEPLOY_KEY)
                                                          .build());

      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              commandStepExecutionSummary);

    } else if (InfrastructureMappingType.AWS_ECS == infrastructureMappingType) {
      CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
      commandStepExecutionSummary.setCodeDeployDeploymentId(CODE_DEPLOY_DEPLOYMENT_ID);
      commandStepExecutionSummary.setClusterName(CLUSTER_NAME);
      commandStepExecutionSummary.setNewInstanceData(
          asList(ContainerServiceData.builder().desiredCount(1).name("ecsNew").previousCount(1).build()));
      commandStepExecutionSummary.setOldInstanceData(
          asList(ContainerServiceData.builder().desiredCount(1).name("ecsOld").previousCount(1).build()));

      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              commandStepExecutionSummary);
    }

    return initPhaseExecutionSummary(stepExecutionSummaries, phaseStepExecutionSummaryString);
  }

  private PhaseExecutionSummary initPhaseExecutionSummary(
      List<StepExecutionSummary> stepExecutionSummaries, String phaseStepExecutionSummaryString) {
    PhaseStepExecutionSummary phaseStepExecutionSummary = new PhaseStepExecutionSummary();
    phaseStepExecutionSummary.setStepExecutionSummaryList(stepExecutionSummaries);

    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    phaseExecutionSummary.setPhaseStepExecutionSummaryMap(
        Collections.singletonMap(phaseStepExecutionSummaryString, phaseStepExecutionSummary));
    return phaseExecutionSummary;
  }

  public PhaseExecutionSummary initKubernetesPhaseExecutionSummary(
      InfrastructureMappingType infrastructureMappingType, String phaseStepExecutionSummaryString, boolean helm) {
    List<StepExecutionSummary> stepExecutionSummaries = null;

    if (InfrastructureMappingType.GCP_KUBERNETES == infrastructureMappingType) {
      if (helm) {
        HelmSetupExecutionSummary helmSetupExecutionSummary =
            new HelmSetupExecutionSummary("version1", 1, 0, 0, "default", null, Arrays.asList("default"));

        stepExecutionSummaries =
            asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
                helmSetupExecutionSummary);

      } else {
        CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
        commandStepExecutionSummary.setClusterName(CLUSTER_NAME);
        commandStepExecutionSummary.setNewInstanceData(ImmutableList.of(
            ContainerServiceData.builder().desiredCount(1).name("kubernetesNew").previousCount(1).build()));
        commandStepExecutionSummary.setOldInstanceData(ImmutableList.of(
            ContainerServiceData.builder().desiredCount(1).name("kubernetesOld").previousCount(1).build()));

        stepExecutionSummaries =
            asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
                commandStepExecutionSummary);
      }
    }

    return initPhaseExecutionSummary(stepExecutionSummaries, phaseStepExecutionSummaryString);
  }
}
