package software.wings.service.impl.instance;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;

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
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.command.CodeDeployParams;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder;
import software.wings.sm.PhaseExecutionSummary;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.StepExecutionSummary;
import software.wings.sm.StepExecutionSummary.StepExecutionSummaryBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InstanceHelperTestHelper {
  public InstanceHelperTestHelper() {}

  private List<InstanceStatusSummary> getInstanceStatusSummariesForPDC() {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .withHost(HostElement.Builder.aHostElement()
                                                                           .withUuid("host_1")
                                                                           .withHostName("hostName1")
                                                                           .withInstanceId("instance1")
                                                                           .withPublicDns("instance1")
                                                                           .build())
                                                             .withUuid("instance_1")
                                                             .build())
                                    .build());

    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .withHost(HostElement.Builder.aHostElement()
                                                                           .withUuid("host_2")
                                                                           .withHostName("hostName2")
                                                                           .withInstanceId("instance2")
                                                                           .withPublicDns("instance2")
                                                                           .build())
                                                             .withUuid("instance_2")
                                                             .build())
                                    .build());

    return instanceStatusSummaries;
  }

  private List<InstanceStatusSummary> getInstanceStatusSummariesForAws() {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(
        InstanceStatusSummaryBuilder.anInstanceStatusSummary()
            .withStatus(ExecutionStatus.SUCCESS)
            .withInstanceElement(
                InstanceElement.Builder.anInstanceElement()
                    .withHost(HostElement.Builder.aHostElement().withEc2Instance(InstanceHelperTest.instance1).build())
                    .withUuid("instance_1")
                    .build())
            .build());

    instanceStatusSummaries.add(
        InstanceStatusSummaryBuilder.anInstanceStatusSummary()
            .withStatus(ExecutionStatus.SUCCESS)
            .withInstanceElement(
                InstanceElement.Builder.anInstanceElement()
                    .withHost(HostElement.Builder.aHostElement().withEc2Instance(InstanceHelperTest.instance2).build())
                    .withUuid("instance_2")
                    .build())
            .build());

    return instanceStatusSummaries;
  }

  private List<InstanceStatusSummary> getInstanceStatusSummariesForGCP() {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .withHost(HostElement.Builder.aHostElement().build())
                                                             .withUuid("instance_1")
                                                             .build())
                                    .build());

    instanceStatusSummaries.add(InstanceStatusSummaryBuilder.anInstanceStatusSummary()
                                    .withStatus(ExecutionStatus.SUCCESS)
                                    .withInstanceElement(InstanceElement.Builder.anInstanceElement()
                                                             .withHost(HostElement.Builder.aHostElement().build())
                                                             .withUuid("instance_2")
                                                             .build())
                                    .build());

    return instanceStatusSummaries;
  }

  private List<InstanceStatusSummary> getInstanceStatusSummariesForECS() {
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    instanceStatusSummaries.add(
        InstanceStatusSummaryBuilder.anInstanceStatusSummary()
            .withStatus(ExecutionStatus.SUCCESS)
            .withInstanceElement(
                InstanceElement.Builder.anInstanceElement()
                    .withHost(HostElement.Builder.aHostElement().withEc2Instance(InstanceHelperTest.instance1).build())
                    .withUuid("instance_1")
                    .build())
            .build());

    instanceStatusSummaries.add(
        InstanceStatusSummaryBuilder.anInstanceStatusSummary()
            .withStatus(ExecutionStatus.SUCCESS)
            .withInstanceElement(
                InstanceElement.Builder.anInstanceElement()
                    .withHost(HostElement.Builder.aHostElement().withEc2Instance(InstanceHelperTest.instance2).build())
                    .withUuid("instance_2")
                    .build())
            .build());

    return instanceStatusSummaries;
  }

  public PhaseExecutionData initExecutionSummary(InfrastructureMappingType infrastructureMappingType,
      String phaseStepExecutionSummaryString, long endTime, String deploymentType) {
    List<InstanceStatusSummary> instanceStatusSummaries = null;
    List<StepExecutionSummary> stepExecutionSummaries = null;

    if (InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.equals(infrastructureMappingType)) {
      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build());

      instanceStatusSummaries = getInstanceStatusSummariesForPDC();

    } else if (InfrastructureMappingType.AWS_SSH.equals(infrastructureMappingType)) {
      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build());

      instanceStatusSummaries = getInstanceStatusSummariesForAws();
    } else if (InfrastructureMappingType.AWS_AMI.equals(infrastructureMappingType)) {
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

      instanceStatusSummaries = getInstanceStatusSummariesForAws();
    } else if (InfrastructureMappingType.AWS_AWS_CODEDEPLOY.equals(infrastructureMappingType)) {
      CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
      commandStepExecutionSummary.setCodeDeployDeploymentId(InstanceHelperTest.CODE_DEPLOY_DEPLOYMENT_ID);
      commandStepExecutionSummary.setCodeDeployParams(
          CodeDeployParams.builder()
              .applicationName(InstanceHelperTest.CODE_DEPLOY_APP_NAME)
              .deploymentGroupName(InstanceHelperTest.CODE_DEPLOY_GROUP_NAME)
              .key(InstanceHelperTest.CODE_DEPLOY_key)
              .build());

      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              commandStepExecutionSummary);

      instanceStatusSummaries = getInstanceStatusSummariesForAws();
    } else if (InfrastructureMappingType.AWS_ECS.equals(infrastructureMappingType)) {
      CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
      commandStepExecutionSummary.setCodeDeployDeploymentId(InstanceHelperTest.CODE_DEPLOY_DEPLOYMENT_ID);
      commandStepExecutionSummary.setClusterName(InstanceHelperTest.CLUSTER_NAME);
      commandStepExecutionSummary.setNewInstanceData(
          asList(ContainerServiceData.builder().desiredCount(1).name("ecsNew").previousCount(1).build()));
      commandStepExecutionSummary.setOldInstanceData(
          asList(ContainerServiceData.builder().desiredCount(1).name("ecsOld").previousCount(1).build()));

      stepExecutionSummaries =
          asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
              commandStepExecutionSummary);

      instanceStatusSummaries = getInstanceStatusSummariesForAws();
    }

    return initExecutionSummary(
        instanceStatusSummaries, stepExecutionSummaries, phaseStepExecutionSummaryString, endTime, deploymentType);
  }

  public PhaseExecutionData initKubernetesExecutionSummary(InfrastructureMappingType infrastructureMappingType,
      String phaseStepExecutionSummaryString, long endTime, String deploymentType, boolean helm) {
    List<InstanceStatusSummary> instanceStatusSummaries = null;
    List<StepExecutionSummary> stepExecutionSummaries = null;

    if (InfrastructureMappingType.GCP_KUBERNETES.equals(infrastructureMappingType)) {
      if (helm) {
        HelmSetupExecutionSummary helmSetupExecutionSummary =
            new HelmSetupExecutionSummary("version1", Integer.valueOf(1), Integer.valueOf(0), 0);

        stepExecutionSummaries =
            asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
                helmSetupExecutionSummary);

      } else {
        CommandStepExecutionSummary commandStepExecutionSummary = new CommandStepExecutionSummary();
        commandStepExecutionSummary.setClusterName(InstanceHelperTest.CLUSTER_NAME);
        commandStepExecutionSummary.setNewInstanceData(
            asList(ContainerServiceData.builder().desiredCount(1).name("kubernetesNew").previousCount(1).build()));
        commandStepExecutionSummary.setOldInstanceData(
            asList(ContainerServiceData.builder().desiredCount(1).name("kubernetesOld").previousCount(1).build()));

        stepExecutionSummaries =
            asList(StepExecutionSummaryBuilder.aStepExecutionSummary().withStatus(ExecutionStatus.SUCCESS).build(),
                commandStepExecutionSummary);
      }

      instanceStatusSummaries = getInstanceStatusSummariesForGCP();
    }

    return initExecutionSummary(
        instanceStatusSummaries, stepExecutionSummaries, phaseStepExecutionSummaryString, endTime, deploymentType);
  }

  private PhaseExecutionData initExecutionSummary(List<InstanceStatusSummary> instanceStatusSummaries,
      List<StepExecutionSummary> stepExecutionSummaries, String phaseStepExecutionSummaryString, long endTime,
      String deploymentType) {
    ElementExecutionSummary summary = ElementExecutionSummaryBuilder.anElementExecutionSummary()
                                          .withInstanceStatusSummaries(instanceStatusSummaries)
                                          .withStatus(ExecutionStatus.SUCCESS)
                                          .build();

    // - This mocking will be used to test getDeployPhaseStep(phaseExecutionData, Constants.DEPLOY_SERVICE);
    PhaseStepExecutionSummary phaseStepExecutionSummary = new PhaseStepExecutionSummary();
    phaseStepExecutionSummary.setStepExecutionSummaryList(stepExecutionSummaries);

    PhaseExecutionSummary phaseExecutionSummary = new PhaseExecutionSummary();
    phaseExecutionSummary.setPhaseStepExecutionSummaryMap(
        Collections.singletonMap(phaseStepExecutionSummaryString, phaseStepExecutionSummary));
    // ---------------------------------------

    return PhaseExecutionDataBuilder.aPhaseExecutionData()
        .withServiceId(InstanceHelperTest.SERVICE_ID)
        .withServiceName("serviceName")
        .withElementStatusSummary(Lists.newArrayList(summary))
        .withComputeProviderId("computeProvider_1")
        .withComputeProviderName("computeProviderName")
        .withInfraMappingId(InstanceHelperTest.INFRA_MAP_ID)
        .withEndTs(endTime)
        .withPhaseExecutionSummary(phaseExecutionSummary)
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
            .accountId(InstanceHelperTest.ACCOUNT_ID)
            .serviceId(InstanceHelperTest.SERVICE_ID)
            .serviceName("serviceName")
            .appName("app1")
            .infraMappingId(InstanceHelperTest.INFRA_MAP_ID)
            .infraMappingType(mappingType.getName())
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
            .lastWorkflowExecutionId(InstanceHelperTest.WORKFLOW_EXECUTION_ID)
            .lastWorkflowExecutionName("workflow1")
            .appId(InstanceHelperTest.APP_ID)
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
            .accountId(InstanceHelperTest.ACCOUNT_ID)
            .serviceId(InstanceHelperTest.SERVICE_ID)
            .serviceName("serviceName")
            .appName("app1")
            .infraMappingId(InstanceHelperTest.INFRA_MAP_ID)
            .infraMappingType(mappingType.getName())
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
            .lastWorkflowExecutionId(InstanceHelperTest.WORKFLOW_EXECUTION_ID)
            .lastWorkflowExecutionName("workflow1")
            .appId(InstanceHelperTest.APP_ID)
            .createdAt(0)
            .lastUpdatedAt(0)
            .instanceInfo(instanceInfo2)
            .build());
  }
}
