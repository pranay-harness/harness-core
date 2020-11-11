package io.harness.executionplan;

import static io.harness.common.BuildEnvironmentConstants.DRONE_BUILD_NUMBER;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_BRANCH;
import static io.harness.common.CIExecutionConstants.ARGS_PREFIX;
import static io.harness.common.CIExecutionConstants.CI_PIPELINE_CONFIG;
import static io.harness.common.CIExecutionConstants.DEFAULT_LIMIT_MEMORY_MIB;
import static io.harness.common.CIExecutionConstants.DEFAULT_LIMIT_MILLI_CPU;
import static io.harness.common.CIExecutionConstants.ENTRYPOINT_PREFIX;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_DEPTH_ATTRIBUTE;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_IMAGE;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_MANUAL_DEPTH;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_STEP_ID;
import static io.harness.common.CIExecutionConstants.GIT_CLONE_STEP_NAME;
import static io.harness.common.CIExecutionConstants.HARNESS_WORKSPACE;
import static io.harness.common.CIExecutionConstants.ID_PREFIX;
import static io.harness.common.CIExecutionConstants.IMAGE_PREFIX;
import static io.harness.common.CIExecutionConstants.PLUGIN_ENV_PREFIX;
import static io.harness.common.CIExecutionConstants.PORT_PREFIX;
import static io.harness.common.CIExecutionConstants.PORT_STARTING_RANGE;
import static io.harness.common.CIExecutionConstants.SERVICE_ARG_COMMAND;
import static io.harness.common.CIExecutionConstants.STEP_COMMAND;
import static io.harness.common.CIExecutionConstants.STEP_REQUEST_MEMORY_MIB;
import static io.harness.common.CIExecutionConstants.STEP_REQUEST_MILLI_CPU;
import static io.harness.delegate.beans.ci.pod.CIContainerType.PLUGIN;
import static io.harness.delegate.beans.ci.pod.CIContainerType.RUN;
import static io.harness.delegate.beans.ci.pod.CIContainerType.SERVICE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import com.google.inject.Singleton;

import graph.StepInfoGraph;
import io.harness.beans.dependencies.CIServiceInfo;
import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.ExecutionSource;
import io.harness.beans.execution.ManualExecutionSource;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.Repository;
import io.harness.beans.execution.WebhookBaseAttributes;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.executionargs.ExecutionArgs;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.EcrConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.beans.yaml.extended.CustomSecretVariable;
import io.harness.beans.yaml.extended.CustomTextVariable;
import io.harness.beans.yaml.extended.CustomVariable;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.common.CIExecutionConstants;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams.CIK8ContainerParamsBuilder;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDTO;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerAuthenticationDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.gitconnector.GitAuthType;
import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.gitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesUserNamePasswordDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.k8s.model.ImageDetails;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.extended.ci.codebase.CodeBase;
import io.harness.yaml.extended.ci.codebase.CodeBaseType;
import io.harness.yaml.extended.ci.codebase.impl.GitHubCodeBase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class CIExecutionPlanTestHelper {
  private static final String BUILD_STAGE_NAME = "buildStage";
  private static final String ENV_SETUP_NAME = "envSetupName";
  private static final String BUILD_SCRIPT = "mvn clean install";
  private static final long BUILD_NUMBER = 20;

  private static final String RUN_STEP_IMAGE = "maven:3.6.3-jdk-8";
  private static final String RUN_STEP_CONNECTOR = "run";
  private static final String RUN_STEP_ID = "step-2";

  private static final String PLUGIN_STEP_IMAGE = "plugins/git";
  private static final String PLUGIN_STEP_ID = "step-3";
  private static final Integer PLUGIN_STEP_LIMIT_MEM = 50;
  private static final Integer PLUGIN_STEP_LIMIT_CPU = 100;
  private static final String PLUGIN_ENV_VAR = "foo";
  private static final String PLUGIN_ENV_VAL = "bar";

  private static final String SERVICE_ID = "db";
  private static final String SERVICE_CTR_NAME = "service-0";
  private static final String SERVICE_VOLUME_NAME = "service-0";
  private static final Integer SERVICE_LIMIT_MEM = 60;
  private static final Integer SERVICE_LIMIT_CPU = 80;
  private static final String SERVICE_IMAGE = "redis";
  private static final String SERVICE_ENTRYPOINT = "redis";
  private static final String SERVICE_ARGS = "start";

  private static final String REPO_NAMESPACE = "wings";
  private static final String REPO_NAME = "portal";
  private static final String REPO_BRANCH = "master";

  private static final String COMMIT_MESSAGE = "foo=bar";
  private static final String COMMIT_LINK = "foo/bar";
  private static final String COMMIT = "e9a0d31c5ac677ec1e06fb3ab69cd1d2cc62a74a";

  private static final String MOUNT_PATH = "/step-exec";
  private static final String VOLUME_NAME = "step-exec";
  private static final String WORK_DIR = "/step-exec/workspace";

  public static final String GIT_CONNECTOR = "git-connector";
  private static final String CLONE_STEP_ID = "step-1";
  private static final String GIT_PLUGIN_DEPTH_ENV = "PLUGIN_DEPTH";
  private static final Integer GIT_STEP_LIMIT_MEM = 200;
  private static final Integer GIT_STEP_LIMIT_CPU = 200;

  private final ImageDetails imageDetails = ImageDetails.builder().name("maven").tag("3.6.3-jdk-8").build();

  public List<ScriptInfo> getBuildCommandSteps() {
    return singletonList(ScriptInfo.builder().scriptString(BUILD_SCRIPT).build());
  }

  public List<ExecutionWrapper> getExpectedExecutionWrappers() {
    List<ExecutionWrapper> executionWrappers = new ArrayList<>();
    executionWrappers.add(StepElement.builder()
                              .identifier("liteEngineTask1")
                              .stepSpecType(LiteEngineTaskStepInfo.builder()
                                                .identifier("liteEngineTask1")
                                                .buildJobEnvInfo(getCIBuildJobEnvInfoOnFirstPod())
                                                .usePVC(true)
                                                .steps(getExpectedExecutionElementWithoutCleanup())
                                                .accountId("accountId")
                                                .build())
                              .build());

    return executionWrappers;
  }

  public CodeBase getCICodebase() {
    return CodeBase.builder()
        .codeBaseType(CodeBaseType.GIT_HUB)
        .codeBaseSpec(GitHubCodeBase.builder().connectorRef(GIT_CONNECTOR).build())
        .build();
  }

  public ConnectorDetails getGitConnector() {
    ConnectorInfoDTO connectorInfo = getGitConnectorDTO().getConnectorInfo();
    return ConnectorDetails.builder()
        .connectorConfig(connectorInfo.getConnectorConfig())
        .connectorType(connectorInfo.getConnectorType())
        .identifier(connectorInfo.getIdentifier())
        .projectIdentifier(connectorInfo.getProjectIdentifier())
        .orgIdentifier(connectorInfo.getOrgIdentifier())
        .build();
  }

  public LiteEngineTaskStepInfo getExpectedLiteEngineTaskInfoOnFirstPod() {
    return LiteEngineTaskStepInfo.builder()
        .identifier("liteEngineTask1")
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnFirstPod())
        .usePVC(true)
        .steps(getExpectedExecutionElement(false))
        .accountId("accountId")
        .ciCodebase(getCICodebase())
        .build();
  }

  public LiteEngineTaskStepInfo getExpectedLiteEngineTaskInfoOnFirstPodWithSetCallbackId() {
    return LiteEngineTaskStepInfo.builder()
        .identifier("liteEngineTask1")
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnFirstPod())
        .usePVC(true)
        .steps(getExpectedExecutionElement(true))
        .accountId("accountId")
        .ciCodebase(getCICodebase())
        .build();
  }

  public LiteEngineTaskStepInfo getExpectedLiteEngineTaskInfoOnOtherPods() {
    return LiteEngineTaskStepInfo.builder()
        .identifier("liteEngineTask2")
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnOtherPods())
        .usePVC(true)
        .steps(getExecutionElement())
        .accountId("accountId")
        .build();
  }

  public BuildEnvSetupStepInfo getBuildEnvSetupStepInfoOnFirstPod() {
    return BuildEnvSetupStepInfo.builder()
        .identifier(ENV_SETUP_NAME)
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnFirstPod())
        .branchName("master")
        .gitConnectorIdentifier("testGitConnector")
        .build();
  }
  public BuildEnvSetupStepInfo getBuildEnvSetupStepInfoOnOtherPods() {
    return BuildEnvSetupStepInfo.builder()
        .identifier(ENV_SETUP_NAME)
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnOtherPods())
        .build();
  }

  public BuildJobEnvInfo getCIBuildJobEnvInfoOnFirstPod() {
    return K8BuildJobEnvInfo.builder()
        .workDir("/root")
        .podsSetupInfo(getCIPodsSetupInfoOnFirstPod())
        .publishStepConnectorIdentifier(getPublishArtifactConnectorIds())
        .build();
  }

  public BuildJobEnvInfo getCIBuildJobEnvInfoOnOtherPods() {
    return K8BuildJobEnvInfo.builder()
        .workDir("/root")
        .podsSetupInfo(getCIPodsSetupInfoOnOtherPods())
        .publishStepConnectorIdentifier(getPublishArtifactConnectorIds())
        .build();
  }

  public K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfoOnFirstPod() {
    List<PodSetupInfo> pods = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<Integer> serviceGrpcPortList = new ArrayList<>();
    Integer index = 1;
    pods.add(PodSetupInfo.builder()
                 .name("")
                 .pvcParams(PVCParams.builder()
                                .volumeName("step-exec")
                                .claimName("")
                                .isPresent(false)
                                .sizeMib(CIExecutionConstants.PVC_DEFAULT_STORAGE_SIZE)
                                .storageClass(CIExecutionConstants.PVC_DEFAULT_STORAGE_CLASS)
                                .build())
                 .podSetupParams(
                     PodSetupInfo.PodSetupParams.builder()
                         .containerDefinitionInfos(asList(getServiceContainer(), getGitPluginStepContainer(index),
                             getRunStepContainer(index + 1), getPluginStepContainer(index + 2)))
                         .build())
                 .stageMemoryRequest(250)
                 .stageCpuRequest(300)
                 .serviceIdList(Collections.singletonList(SERVICE_ID))
                 .serviceGrpcPortList(Collections.singletonList(PORT_STARTING_RANGE))
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  public K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfoOnOtherPods() {
    List<PodSetupInfo> pods = new ArrayList<>();
    List<String> serviceIds = new ArrayList<>();
    List<Integer> serviceGrpcPortList = new ArrayList<>();

    Integer index = 1;
    pods.add(PodSetupInfo.builder()
                 .name("")
                 .pvcParams(PVCParams.builder()
                                .volumeName("step-exec")
                                .claimName("buildnumber22850")
                                .isPresent(true)
                                .sizeMib(CIExecutionConstants.PVC_DEFAULT_STORAGE_SIZE)
                                .storageClass(CIExecutionConstants.PVC_DEFAULT_STORAGE_CLASS)
                                .build())
                 .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                     .containerDefinitionInfos(asList(getServiceContainer(), getRunStepContainer(index),
                                         getPluginStepContainer(index + 1)))
                                     .build())
                 .stageMemoryRequest(250)
                 .stageCpuRequest(300)
                 .serviceIdList(serviceIds)
                 .serviceGrpcPortList(serviceGrpcPortList)
                 .serviceIdList(Collections.singletonList(SERVICE_ID))
                 .serviceGrpcPortList(Collections.singletonList(PORT_STARTING_RANGE))
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  public StepInfoGraph getStepsGraph() {
    return StepInfoGraph.builder()
        .steps(asList(getBuildEnvSetupStepInfoOnFirstPod(),
            TestStepInfo.builder().identifier(BUILD_STAGE_NAME).scriptInfos(getBuildCommandSteps()).build()))
        .build();
  }

  public ExecutionElement getExecutionElement() {
    List<ExecutionWrapper> executionSectionList = getExecutionSectionsWithLESteps();
    executionSectionList.addAll(getCleanupStep());
    return ExecutionElement.builder().steps(executionSectionList).build();
  }

  public ExecutionElement getExpectedExecutionElementWithoutCleanup() {
    List<ExecutionWrapper> executionSectionList = getExpectedExecutionSectionsWithLESteps(false);
    return ExecutionElement.builder().steps(executionSectionList).build();
  }

  public ExecutionElement getExpectedExecutionElement(boolean setCallbackId) {
    List<ExecutionWrapper> executionSectionList = getExpectedExecutionSectionsWithLESteps(setCallbackId);
    executionSectionList.addAll(getCleanupStep());
    return ExecutionElement.builder().steps(executionSectionList).build();
  }

  private DependencyElement getServiceDependencyElement() {
    return DependencyElement.builder()
        .identifier(SERVICE_ID)
        .dependencySpecType(
            CIServiceInfo.builder()
                .identifier(SERVICE_ID)
                .args(Collections.singletonList(SERVICE_ARGS))
                .entrypoint(Collections.singletonList(SERVICE_ENTRYPOINT))
                .image(SERVICE_IMAGE)
                .resources(
                    ContainerResource.builder()
                        .limit(
                            ContainerResource.Limit.builder().cpu(SERVICE_LIMIT_CPU).memory(SERVICE_LIMIT_MEM).build())
                        .build())
                .build())
        .build();
  }

  private StepElement getRunStepElement(Integer index) {
    return StepElement.builder()
        .identifier(RUN_STEP_ID)
        .type("run")
        .name("testScript1")
        .stepSpecType(RunStepInfo.builder()
                          .identifier(RUN_STEP_ID)
                          .name("testScript1")
                          .command(singletonList("./test-script1.sh"))
                          .callbackId("test-p1-callbackId")
                          .image(RUN_STEP_IMAGE)
                          .connector(RUN_STEP_CONNECTOR)
                          .port(PORT_STARTING_RANGE + index)
                          .build())
        .build();
  }

  private ContainerDefinitionInfo getServiceContainer() {
    Integer port = PORT_STARTING_RANGE;
    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(VOLUME_NAME, MOUNT_PATH);
    volumeToMountPath.put(SERVICE_VOLUME_NAME, "/" + SERVICE_VOLUME_NAME);

    List<String> args = Arrays.asList(SERVICE_ARG_COMMAND, ID_PREFIX, SERVICE_ID, IMAGE_PREFIX, SERVICE_IMAGE,
        ENTRYPOINT_PREFIX, SERVICE_ENTRYPOINT, ARGS_PREFIX, SERVICE_ARGS, PORT_PREFIX, port.toString());

    return ContainerDefinitionInfo.builder()
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(ImageDetails.builder().name(SERVICE_IMAGE).tag("").build())
                                   .build())
        .name(SERVICE_CTR_NAME)
        .containerType(SERVICE)
        .args(args)
        .commands(asList(STEP_COMMAND))
        .ports(asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(SERVICE_LIMIT_CPU)
                                     .resourceRequestMemoryMiB(SERVICE_LIMIT_MEM)
                                     .resourceLimitMilliCpu(SERVICE_LIMIT_CPU)
                                     .resourceLimitMemoryMiB(SERVICE_LIMIT_MEM)
                                     .build())
        .volumeToMountPath(volumeToMountPath)
        .workingDirectory("/" + SERVICE_VOLUME_NAME)
        .build();
  }

  public CIK8ContainerParams getServiceCIK8Container() {
    Integer port = PORT_STARTING_RANGE;
    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(VOLUME_NAME, MOUNT_PATH);
    volumeToMountPath.put(SERVICE_VOLUME_NAME, "/" + SERVICE_VOLUME_NAME);

    List<String> args = Arrays.asList(SERVICE_ARG_COMMAND, ID_PREFIX, SERVICE_ID, IMAGE_PREFIX, SERVICE_IMAGE,
        ENTRYPOINT_PREFIX, SERVICE_ENTRYPOINT, ARGS_PREFIX, SERVICE_ARGS, PORT_PREFIX, port.toString());

    return CIK8ContainerParams.builder()
        .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                       .imageDetails(ImageDetails.builder().name(SERVICE_IMAGE).tag("").build())
                                       .build())
        .name(SERVICE_CTR_NAME)
        .containerType(SERVICE)
        .args(args)
        .commands(asList(STEP_COMMAND))
        .ports(asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(SERVICE_LIMIT_CPU)
                                     .resourceRequestMemoryMiB(SERVICE_LIMIT_MEM)
                                     .resourceLimitMilliCpu(SERVICE_LIMIT_CPU)
                                     .resourceLimitMemoryMiB(SERVICE_LIMIT_MEM)
                                     .build())
        .workingDir("/" + SERVICE_VOLUME_NAME)
        .volumeToMountPath(volumeToMountPath)
        .build();
  }

  private ContainerDefinitionInfo getRunStepContainer(Integer index) {
    Integer port = PORT_STARTING_RANGE + index;
    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(VOLUME_NAME, MOUNT_PATH);
    return ContainerDefinitionInfo.builder()
        .containerImageDetails(
            ContainerImageDetails.builder().connectorIdentifier(RUN_STEP_CONNECTOR).imageDetails(imageDetails).build())
        .name("step-" + index.toString())
        .containerType(RUN)
        .args(Arrays.asList(PORT_PREFIX, port.toString()))
        .commands(Arrays.asList(STEP_COMMAND))
        .ports(Arrays.asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
                                     .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
                                     .resourceLimitMilliCpu(DEFAULT_LIMIT_MILLI_CPU)
                                     .resourceLimitMemoryMiB(DEFAULT_LIMIT_MEMORY_MIB)
                                     .build())
        .envVars(getEnvVariables())
        .secretVariables(getCustomSecretVariable())
        .workingDirectory(WORK_DIR)
        .volumeToMountPath(volumeToMountPath)
        .build();
  }

  public CIK8ContainerParamsBuilder getRunStepCIK8Container() {
    Integer port = PORT_STARTING_RANGE + 2;
    return CIK8ContainerParams.builder()
        .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                       .imageDetails(imageDetails)
                                       .imageConnectorDetails(ConnectorDetails.builder().build())
                                       .build())
        .name(RUN_STEP_ID)
        .containerType(RUN)
        .args(asList(PORT_PREFIX, port.toString()))
        .commands(asList(STEP_COMMAND))
        .ports(asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
                                     .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
                                     .resourceLimitMilliCpu(DEFAULT_LIMIT_MILLI_CPU)
                                     .resourceLimitMemoryMiB(DEFAULT_LIMIT_MEMORY_MIB)
                                     .build())
        .envVars(getEnvVariables());
  }

  private StepElement getPluginStepElement(Integer index) {
    Map<String, String> settings = new HashMap<>();
    settings.put(PLUGIN_ENV_VAR, PLUGIN_ENV_VAL);
    return StepElement.builder()
        .identifier(PLUGIN_STEP_ID)
        .type("plugin")
        .name("testPlugin")
        .stepSpecType(PluginStepInfo.builder()
                          .identifier(PLUGIN_STEP_ID)
                          .name("testPlugin")
                          .callbackId("test-p1-callbackId")
                          .image(PLUGIN_STEP_IMAGE)
                          .resources(ContainerResource.builder()
                                         .limit(ContainerResource.Limit.builder()
                                                    .cpu(PLUGIN_STEP_LIMIT_CPU)
                                                    .memory(PLUGIN_STEP_LIMIT_MEM)
                                                    .build())
                                         .build())
                          .port(PORT_STARTING_RANGE + index)
                          .settings(settings)
                          .build())
        .build();
  }

  private ContainerDefinitionInfo getPluginStepContainer(Integer index) {
    Map<String, String> envVar = new HashMap<>();
    envVar.put(PLUGIN_ENV_PREFIX + PLUGIN_ENV_VAR.toUpperCase(), PLUGIN_ENV_VAL);
    envVar.put("DRONE_BUILD_NUMBER", Long.toString(BUILD_NUMBER));
    envVar.put(DRONE_COMMIT_BRANCH, REPO_BRANCH);
    envVar.put("HARNESS_WORKSPACE", WORK_DIR);

    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(VOLUME_NAME, MOUNT_PATH);
    Integer port = PORT_STARTING_RANGE + index;
    return ContainerDefinitionInfo.builder()
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(ImageDetails.builder().name(PLUGIN_STEP_IMAGE).tag("").build())
                                   .build())
        .name("step-" + index.toString())
        .containerType(PLUGIN)
        .args(Arrays.asList(PORT_PREFIX, port.toString()))
        .commands(Arrays.asList(STEP_COMMAND))
        .ports(Arrays.asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
                                     .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
                                     .resourceLimitMilliCpu(PLUGIN_STEP_LIMIT_CPU)
                                     .resourceLimitMemoryMiB(PLUGIN_STEP_LIMIT_MEM)
                                     .build())
        .envVars(envVar)
        .workingDirectory(WORK_DIR)
        .volumeToMountPath(volumeToMountPath)
        .build();
  }

  private ContainerDefinitionInfo getGitPluginStepContainer(Integer index) {
    Map<String, String> envVar = new HashMap<>();
    envVar.put(HARNESS_WORKSPACE, WORK_DIR);
    envVar.put(GIT_PLUGIN_DEPTH_ENV, GIT_CLONE_MANUAL_DEPTH.toString());
    envVar.put(DRONE_COMMIT_BRANCH, REPO_BRANCH);
    envVar.put(DRONE_BUILD_NUMBER, Long.toString(BUILD_NUMBER));

    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(VOLUME_NAME, MOUNT_PATH);
    Integer port = PORT_STARTING_RANGE + index;
    return ContainerDefinitionInfo.builder()
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(ImageDetails.builder().name(GIT_CLONE_IMAGE).tag("").build())
                                   .build())
        .name("step-" + index.toString())
        .containerType(PLUGIN)
        .args(Arrays.asList(PORT_PREFIX, port.toString()))
        .commands(Arrays.asList(STEP_COMMAND))
        .ports(Arrays.asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
                                     .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
                                     .resourceLimitMilliCpu(GIT_STEP_LIMIT_CPU)
                                     .resourceLimitMemoryMiB(GIT_STEP_LIMIT_MEM)
                                     .build())
        .envVars(envVar)
        .workingDirectory(WORK_DIR)
        .volumeToMountPath(volumeToMountPath)
        .build();
  }

  public CIK8ContainerParamsBuilder getGitCloneStepCIK8Container() {
    Map<String, String> envVar = new HashMap<>();
    envVar.put(PLUGIN_ENV_PREFIX + PLUGIN_ENV_VAR.toUpperCase(), PLUGIN_ENV_VAL);
    envVar.put(DRONE_BUILD_NUMBER, Long.toString(BUILD_NUMBER));
    envVar.put(HARNESS_WORKSPACE, WORK_DIR);
    envVar.put(DRONE_COMMIT_BRANCH, REPO_BRANCH);

    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(VOLUME_NAME, MOUNT_PATH);
    Integer port = PORT_STARTING_RANGE + 1;
    return CIK8ContainerParams.builder()
        .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                       .imageDetails(ImageDetails.builder().name(GIT_CLONE_IMAGE).tag("").build())
                                       .build())
        .name(CLONE_STEP_ID)
        .containerType(PLUGIN)
        .args(asList(PORT_PREFIX, port.toString()))
        .commands(asList(STEP_COMMAND))
        .ports(asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
                                     .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
                                     .resourceLimitMilliCpu(DEFAULT_LIMIT_MILLI_CPU)
                                     .resourceLimitMemoryMiB(DEFAULT_LIMIT_MEMORY_MIB)
                                     .build())
        .workingDir(WORK_DIR)
        .volumeToMountPath(volumeToMountPath)
        .envVars(envVar);
  }

  public CIK8ContainerParamsBuilder getPluginStepCIK8Container() {
    Map<String, String> envVar = new HashMap<>();
    envVar.put(PLUGIN_ENV_PREFIX + PLUGIN_ENV_VAR.toUpperCase(), PLUGIN_ENV_VAL);
    envVar.put(DRONE_BUILD_NUMBER, Long.toString(BUILD_NUMBER));
    envVar.put(HARNESS_WORKSPACE, WORK_DIR);
    envVar.put(DRONE_COMMIT_BRANCH, REPO_BRANCH);

    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(VOLUME_NAME, MOUNT_PATH);
    Integer port = PORT_STARTING_RANGE + 3;
    return CIK8ContainerParams.builder()
        .imageDetailsWithConnector(ImageDetailsWithConnector.builder()
                                       .imageDetails(ImageDetails.builder().name(PLUGIN_STEP_IMAGE).tag("").build())
                                       .build())
        .name(PLUGIN_STEP_ID)
        .containerType(PLUGIN)
        .args(asList(PORT_PREFIX, port.toString()))
        .commands(asList(STEP_COMMAND))
        .ports(asList(port))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
                                     .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
                                     .resourceLimitMilliCpu(PLUGIN_STEP_LIMIT_CPU)
                                     .resourceLimitMemoryMiB(PLUGIN_STEP_LIMIT_MEM)
                                     .build())
        .workingDir(WORK_DIR)
        .volumeToMountPath(volumeToMountPath)
        .envVars(envVar);
  }

  public StepElement getGitCloneStep(Integer index, boolean setCallbackId) {
    Map<String, String> settings = new HashMap<>();
    settings.put(GIT_CLONE_DEPTH_ATTRIBUTE, GIT_CLONE_MANUAL_DEPTH.toString());
    String callbackId = null;
    if (setCallbackId) {
      callbackId = "test-p1-callbackId";
    }
    return StepElement.builder()
        .identifier(GIT_CLONE_STEP_ID)
        .name(GIT_CLONE_STEP_NAME)
        .type(CIStepInfoType.PLUGIN.name().toLowerCase())
        .stepSpecType(PluginStepInfo.builder()
                          .identifier(GIT_CLONE_STEP_ID)
                          .image(GIT_CLONE_IMAGE)
                          .name(GIT_CLONE_STEP_NAME)
                          .settings(settings)
                          .callbackId(callbackId)
                          .port(PORT_STARTING_RANGE + index)
                          .build())
        .build();
  }

  public List<ExecutionWrapper> getExecutionSectionsWithLESteps() {
    Integer index = 1;
    return new ArrayList<>(Arrays.asList(
        ParallelStepElement.builder()
            .sections(asList(getRunStepElement(index), getPluginStepElement(index + 1)))
            .build(),
        ParallelStepElement.builder()
            .sections(asList(StepElement.builder()
                                 .identifier("publish-1")
                                 .type("publishArtifacts")
                                 .stepSpecType(PublishStepInfo.builder()
                                                   .identifier("publish-1")
                                                   .callbackId("publish-1-callbackId")
                                                   .publishArtifacts(singletonList(
                                                       DockerFileArtifact.builder()
                                                           .connector(GcrConnector.builder()
                                                                          .connectorRef("gcr-connector")
                                                                          .location("us.gcr.io/ci-play/portal:v01")
                                                                          .build())
                                                           .tag("v01")
                                                           .image("ci-play/portal")
                                                           .context("~/")
                                                           .dockerFile("~/Dockerfile")
                                                           .build()))
                                                   .build())
                                 .build(),
                StepElement.builder()
                    .identifier("publish-2")
                    .type("publishArtifacts")
                    .stepSpecType(
                        PublishStepInfo.builder()
                            .identifier("publish-2")
                            .callbackId("publish-2-callbackId")
                            .publishArtifacts(singletonList(
                                DockerFileArtifact.builder()
                                    .connector(
                                        EcrConnector.builder()
                                            .connectorRef("ecr-connector")
                                            .location(
                                                " https://987923132879.dkr.ecr.eu-west-1.amazonaws.com/ci-play/portal:v01")
                                            .build())
                                    .tag("v01")
                                    .image("ci-play/portal")
                                    .context("~/")
                                    .dockerFile("~/Dockerfile")
                                    .build()))
                            .build())
                    .build()))
            .build()));
  }

  public List<ExecutionWrapper> getExpectedExecutionSectionsWithLESteps(boolean setCallbackId) {
    Integer index = 1;
    return new ArrayList<>(Arrays.asList(getGitCloneStep(index, setCallbackId),
        ParallelStepElement.builder()
            .sections(asList(getRunStepElement(index + 1), getPluginStepElement(index + 2)))
            .build(),
        ParallelStepElement.builder()
            .sections(asList(StepElement.builder()
                                 .identifier("publish-1")
                                 .type("publishArtifacts")
                                 .stepSpecType(PublishStepInfo.builder()
                                                   .identifier("publish-1")
                                                   .callbackId("publish-1-callbackId")
                                                   .publishArtifacts(singletonList(
                                                       DockerFileArtifact.builder()
                                                           .connector(GcrConnector.builder()
                                                                          .connectorRef("gcr-connector")
                                                                          .location("us.gcr.io/ci-play/portal:v01")
                                                                          .build())
                                                           .tag("v01")
                                                           .image("ci-play/portal")
                                                           .context("~/")
                                                           .dockerFile("~/Dockerfile")
                                                           .build()))
                                                   .build())
                                 .build(),
                StepElement.builder()
                    .identifier("publish-2")
                    .type("publishArtifacts")
                    .stepSpecType(
                        PublishStepInfo.builder()
                            .identifier("publish-2")
                            .callbackId("publish-2-callbackId")
                            .publishArtifacts(singletonList(
                                DockerFileArtifact.builder()
                                    .connector(
                                        EcrConnector.builder()
                                            .connectorRef("ecr-connector")
                                            .location(
                                                " https://987923132879.dkr.ecr.eu-west-1.amazonaws.com/ci-play/portal:v01")
                                            .build())
                                    .tag("v01")
                                    .image("ci-play/portal")
                                    .context("~/")
                                    .dockerFile("~/Dockerfile")
                                    .build()))
                            .build())
                    .build()))
            .build()));
  }

  public List<ExecutionWrapper> getCleanupStep() {
    return singletonList(StepElement.builder()
                             .identifier("cleanup")
                             .type("cleanup")
                             .stepSpecType(CleanupStepInfo.builder().identifier("cleanup").build())
                             .build());
  }

  public Set<String> getPublishArtifactConnectorIds() {
    Set<String> ids = new HashSet<>();
    ids.add("gcr-connector");
    ids.add("ecr-connector");
    return ids;
  }

  public NgPipelineEntity getCIPipeline() {
    NgPipeline ngPipeline = NgPipeline.builder().stages(getStages()).build();
    return NgPipelineEntity.builder()
        .identifier("testPipelineIdentifier")
        .orgIdentifier("orgIdentifier")
        .projectIdentifier("projectIdentifier")
        .accountId("accountId")
        .ngPipeline(ngPipeline)
        .build();
  }

  private List<StageElementWrapper> getStages() {
    return new ArrayList<>(singletonList(getIntegrationStageElement()));
  }

  public Connector getConnector() {
    return GitConnectorYaml.builder()
        .identifier("testGitConnector")
        .type("git")
        .spec(GitConnectorYaml.Spec.builder()
                  .authScheme(GitConnectorYaml.Spec.AuthScheme.builder().sshKey("testKey").type("ssh").build())
                  .repo("testRepo")
                  .build())
        .build();
  }

  public Container getContainer() {
    return Container.builder()
        .connector("testConnector")
        .identifier("testContainer")
        .imagePath("maven:3.6.3-jdk-8")
        .resources(Container.Resources.builder()
                       .limit(Container.Limit.builder().cpu(1000).memory(1000).build())
                       .reserve(Container.Reserve.builder().cpu(1000).memory(1000).build())
                       .build())
        .build();
  }

  public Infrastructure getInfrastructure() {
    return K8sDirectInfraYaml.builder()
        .type(Infrastructure.Type.KUBERNETES_DIRECT)
        .spec(
            K8sDirectInfraYaml.Spec.builder().connectorRef("testKubernetesCluster").namespace("testNamespace").build())
        .build();
  }
  public StageElement getIntegrationStageElement() {
    return StageElement.builder().identifier("intStageIdentifier").stageType(getIntegrationStage()).build();
  }

  public IntegrationStage getIntegrationStage() {
    return IntegrationStage.builder()
        .identifier("intStageIdentifier")
        .workingDirectory("/root")
        .execution(getExecutionElement())
        .infrastructure(getInfrastructure())
        .customVariables(getCustomVariables())
        .dependencies(Collections.singletonList(getServiceDependencyElement()))
        .build();
  }

  private List<CustomVariable> getCustomVariables() {
    CustomVariable var1 = CustomSecretVariable.builder()
                              .name("VAR1")
                              .value(SecretRefData.builder().identifier("VAR1_secret").scope(Scope.ACCOUNT).build())
                              .build();
    CustomVariable var2 = CustomSecretVariable.builder()
                              .name("VAR2")
                              .value(SecretRefData.builder().identifier("VAR2_secret").scope(Scope.ACCOUNT).build())
                              .build();
    CustomVariable var3 = CustomTextVariable.builder().name("VAR3").value("value3").build();
    CustomVariable var4 = CustomTextVariable.builder().name("VAR4").value("value4").build();
    return asList(var1, var2, var3, var4);
  }

  public Map<String, String> getEnvVariables() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put(DRONE_COMMIT_BRANCH, REPO_BRANCH);
    envVars.put("VAR3", "value3");
    envVars.put("VAR4", "value4");
    envVars.put("HARNESS_WORKSPACE", WORK_DIR);
    envVars.put("DRONE_BUILD_NUMBER", Long.toString(BUILD_NUMBER));
    return envVars;
  }

  public List<CustomSecretVariable> getCustomSecretVariable() {
    List<CustomSecretVariable> secretVariables = new ArrayList<>();
    secretVariables.add(CustomSecretVariable.builder()
                            .name("VAR1")
                            .value(SecretRefData.builder().identifier("VAR1_secret").scope(Scope.ACCOUNT).build())
                            .build());
    secretVariables.add(CustomSecretVariable.builder()
                            .name("VAR2")
                            .value(SecretRefData.builder().identifier("VAR2_secret").scope(Scope.ACCOUNT).build())
                            .build());
    return secretVariables;
  }

  public List<SecretVariableDetails> getSecretVariableDetails() {
    List<SecretVariableDetails> secretVariableDetails = new ArrayList<>();
    secretVariableDetails.add(
        SecretVariableDetails.builder()
            .secretVariableDTO(
                SecretVariableDTO.builder()
                    .name("VAR1")
                    .type(SecretVariableDTO.Type.TEXT)
                    .secret(SecretRefData.builder().identifier("VAR1_secret").scope(Scope.ACCOUNT).build())
                    .build())
            .encryptedDataDetailList(singletonList(EncryptedDataDetail.builder().build()))
            .build());
    secretVariableDetails.add(
        SecretVariableDetails.builder()
            .secretVariableDTO(
                SecretVariableDTO.builder()
                    .name("VAR2")
                    .type(SecretVariableDTO.Type.TEXT)
                    .secret(SecretRefData.builder().identifier("VAR2_secret").scope(Scope.ACCOUNT).build())
                    .build())
            .encryptedDataDetailList(singletonList(EncryptedDataDetail.builder().build()))
            .build());
    return secretVariableDetails;
  }
  public ConnectorDTO getK8sConnectorDTO() {
    return ConnectorDTO.builder()
        .connectorInfo(
            ConnectorInfoDTO.builder()
                .name("k8sConnector")
                .identifier("k8sConnector")
                .connectorType(ConnectorType.KUBERNETES_CLUSTER)
                .connectorConfig(
                    KubernetesClusterConfigDTO.builder()
                        .credential(
                            KubernetesCredentialDTO.builder()
                                .kubernetesCredentialType(KubernetesCredentialType.MANUAL_CREDENTIALS)
                                .config(KubernetesClusterDetailsDTO.builder()
                                            .masterUrl("10.10.10.10")
                                            .auth(KubernetesAuthDTO.builder()
                                                      .authType(KubernetesAuthType.USER_PASSWORD)
                                                      .credentials(
                                                          KubernetesUserNamePasswordDTO.builder()
                                                              .username("username")
                                                              .passwordRef(SecretRefData.builder()
                                                                               .identifier("k8sPassword")
                                                                               .scope(Scope.ACCOUNT)
                                                                               .decryptedValue("password".toCharArray())
                                                                               .build())
                                                              .build())
                                                      .build())
                                            .build())
                                .build())
                        .build())
                .build())
        .build();
  }
  public ConnectorDTO getK8sConnectorFromDelegateDTO() {
    return ConnectorDTO.builder()
        .connectorInfo(
            ConnectorInfoDTO.builder()
                .name("k8sConnector")
                .identifier("k8sConnector")
                .connectorType(ConnectorType.KUBERNETES_CLUSTER)
                .connectorConfig(
                    KubernetesClusterConfigDTO.builder()
                        .credential(KubernetesCredentialDTO.builder()
                                        .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
                                        .config(KubernetesDelegateDetailsDTO.builder().delegateName("delegate").build())
                                        .build())
                        .build())
                .build())
        .build();
  }
  public ConnectorDTO getDockerConnectorDTO() {
    return ConnectorDTO.builder()
        .connectorInfo(
            ConnectorInfoDTO.builder()
                .name("dockerConnector")
                .identifier("dockerConnector")
                .connectorType(ConnectorType.DOCKER)
                .connectorConfig(
                    DockerConnectorDTO.builder()
                        .dockerRegistryUrl("https://index.docker.io/v1/")
                        .auth(DockerAuthenticationDTO.builder()
                                  .authType(DockerAuthType.USER_PASSWORD)
                                  .credentials(
                                      DockerUserNamePasswordDTO.builder()
                                          .username("uName")
                                          .passwordRef(
                                              SecretRefData.builder().decryptedValue("pWord".toCharArray()).build())
                                          .build())
                                  .build())
                        .build())
                .build())
        .build();
  }

  public ConnectorDTO getGitConnectorDTO() {
    return ConnectorDTO.builder()
        .connectorInfo(ConnectorInfoDTO.builder()
                           .name("gitConnector")
                           .identifier("gitConnector")
                           .connectorType(ConnectorType.GIT)
                           .connectorConfig(GitConfigDTO.builder()
                                                .url("https://github.com/wings-software/portal.git")
                                                .branchName("master")
                                                .gitAuthType(GitAuthType.HTTP)
                                                .gitAuth(GitHTTPAuthenticationDTO.builder()
                                                             .username("username")
                                                             .passwordRef(SecretRefData.builder()
                                                                              .identifier("gitPassword")
                                                                              .scope(Scope.ACCOUNT)
                                                                              .decryptedValue("password".toCharArray())
                                                                              .build())
                                                             .build())
                                                .build())
                           .build())
        .build();
  }

  public CIExecutionArgs getCIExecutionArgs() {
    return CIExecutionArgs.builder()
        .executionSource(ManualExecutionSource.builder().branch(REPO_BRANCH).build())
        .buildNumberDetails(BuildNumberDetails.builder().buildNumber(BUILD_NUMBER).build())
        .build();
  }

  public NgPipeline getPipeline() {
    return NgPipeline.builder().ciCodebase(getCICodebase()).build();
  }

  public CIExecutionArgs getPRCIExecutionArgs() {
    WebhookBaseAttributes core =
        WebhookBaseAttributes.builder().link(COMMIT_LINK).message(COMMIT_MESSAGE).after(COMMIT).build();
    Repository repo = Repository.builder().branch(REPO_BRANCH).name(REPO_NAME).namespace(REPO_NAMESPACE).build();

    WebhookEvent webhookEvent = PRWebhookEvent.builder().baseAttributes(core).repository(repo).build();
    ExecutionSource prExecutionSource = WebhookExecutionSource.builder().webhookEvent(webhookEvent).build();
    return CIExecutionArgs.builder()
        .buildNumberDetails(BuildNumberDetails.builder().buildNumber(BUILD_NUMBER).build())
        .executionSource(prExecutionSource)
        .build();
  }

  public Map<String, String> getPRCIExecutionArgsEnvVars() {
    Map<String, String> envVarMap = new HashMap<>();
    envVarMap.put("DRONE_REPO_NAME", REPO_NAME);
    envVarMap.put("DRONE_REPO_NAMESPACE", REPO_NAMESPACE);
    envVarMap.put("DRONE_REPO_OWNER", REPO_NAMESPACE);
    envVarMap.put("DRONE_REPO_PRIVATE", "false");
    envVarMap.put("DRONE_REPO_SCM", "git");
    envVarMap.put("DRONE_REPO_BRANCH", REPO_BRANCH);
    envVarMap.put("DRONE_COMMIT_LINK", COMMIT_LINK);
    envVarMap.put("DRONE_COMMIT_MESSAGE", COMMIT_MESSAGE);
    envVarMap.put("DRONE_BUILD_NUMBER", BUILD_NUMBER + "");
    envVarMap.put("DRONE_BUILD_EVENT", "pull_request");
    envVarMap.put("DRONE_COMMIT_SHA", COMMIT);
    envVarMap.put("DRONE_COMMIT_AFTER", COMMIT);
    envVarMap.put("DRONE_COMMIT", COMMIT);
    return envVarMap;
  }

  public CIExecutionArgs getBranchCIExecutionArgs() {
    WebhookBaseAttributes core = WebhookBaseAttributes.builder().link(COMMIT_LINK).message(COMMIT_MESSAGE).build();
    Repository repo = Repository.builder().branch(REPO_BRANCH).name(REPO_NAME).namespace(REPO_NAMESPACE).build();

    WebhookEvent webhookEvent = BranchWebhookEvent.builder().baseAttributes(core).repository(repo).build();
    ExecutionSource prExecutionSource = WebhookExecutionSource.builder().webhookEvent(webhookEvent).build();
    return CIExecutionArgs.builder()
        .buildNumberDetails(BuildNumberDetails.builder().buildNumber(BUILD_NUMBER).build())
        .executionSource(prExecutionSource)
        .build();
  }

  public Map<String, String> getBranchCIExecutionArgsEnvVars() {
    Map<String, String> envVarMap = new HashMap<>();
    envVarMap.put("DRONE_REPO_NAME", REPO_NAME);
    envVarMap.put("DRONE_REPO_NAMESPACE", REPO_NAMESPACE);
    envVarMap.put("DRONE_REPO_OWNER", REPO_NAMESPACE);
    envVarMap.put("DRONE_REPO_PRIVATE", "false");
    envVarMap.put("DRONE_REPO_SCM", "git");
    envVarMap.put("DRONE_REPO_BRANCH", REPO_BRANCH);
    envVarMap.put("DRONE_COMMIT_LINK", COMMIT_LINK);
    envVarMap.put("DRONE_COMMIT_MESSAGE", COMMIT_MESSAGE);
    envVarMap.put("DRONE_BUILD_NUMBER", BUILD_NUMBER + "");
    envVarMap.put("DRONE_BUILD_EVENT", "push");
    return envVarMap;
  }

  public ExecutionPlanCreationContextImpl getExecutionPlanCreationContextWithExecutionArgs() {
    ExecutionPlanCreationContextImpl context = ExecutionPlanCreationContextImpl.builder().build();
    context.addAttribute(ExecutionArgs.EXEC_ARGS, getCIExecutionArgs());
    context.addAttribute(CI_PIPELINE_CONFIG, getPipeline());
    return context;
  }

  public ExecutionPlanCreationContextImpl getWebhookPlanContextWithExecArgs() {
    ExecutionPlanCreationContextImpl context = ExecutionPlanCreationContextImpl.builder().build();
    context.addAttribute(ExecutionArgs.EXEC_ARGS, getPRCIExecutionArgs());
    context.addAttribute(CI_PIPELINE_CONFIG, getPipeline());
    return context;
  }
}
