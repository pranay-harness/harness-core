package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CICommonPodConstants.MOUNT_PATH;
import static io.harness.common.CICommonPodConstants.STEP_EXEC;
import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.ADDON_IMAGE_TAG;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ID_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.GRPC_SERVICE_PORT_PREFIX;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.INPUT_ARG_PREFIX;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_ARGS;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_CPU;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_MEM;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_IMAGE_NAME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_IMAGE_TAG;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_JFROG_PATH;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_JFROG_VARIABLE;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_PATH;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_VOLUME;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_ARGS;
import static io.harness.common.CIExecutionConstants.SETUP_ADDON_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.SH_COMMAND;
import static io.harness.common.CIExecutionConstants.STAGE_ARG_COMMAND;
import static io.harness.common.CIExecutionConstants.TMP_PATH;
import static io.harness.common.CIExecutionConstants.TMP_PATH_ARG_PREFIX;

import com.google.inject.Singleton;

import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.delegate.beans.ci.pod.ContainerSecrets;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.k8s.model.ImageDetails;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides container parameters for internally used containers
 */

@Singleton
// TODO: fetch constants from config file.
public class InternalContainerParamsProvider {
  public CIK8ContainerParams getSetupAddonContainerParams(ConnectorDetails containerImageConnectorDetails) {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    List<String> args = new ArrayList<>(Collections.singletonList(SETUP_ADDON_ARGS));
    return CIK8ContainerParams.builder()
        .name(SETUP_ADDON_CONTAINER_NAME)
        .containerType(CIContainerType.ADD_ON)
        .imageDetailsWithConnector(
            ImageDetailsWithConnector.builder()
                .imageDetails(ImageDetails.builder().name(ADDON_IMAGE_NAME).tag(ADDON_IMAGE_TAG).build())
                .imageConnectorDetails(containerImageConnectorDetails)
                .build())
        .containerSecrets(ContainerSecrets.builder().build())
        .volumeToMountPath(map)
        .commands(SH_COMMAND)
        .args(args)
        .build();
  }

  public CIK8ContainerParams getLiteEngineContainerParams(ConnectorDetails containerImageConnectorDetails,
      Map<String, ConnectorDetails> publishArtifactConnectors, K8PodDetails k8PodDetails,
      String serializedLiteEngineTaskStepInfo, String serviceToken, Integer stageCpuRequest, Integer stageMemoryRequest,
      List<Integer> serviceGrpcPortList, Map<String, String> logEnvVars) {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    map.put(LITE_ENGINE_VOLUME, LITE_ENGINE_PATH);
    String arg = String.format("%s %s %s %s %s %s", LITE_ENGINE_ARGS, STAGE_ARG_COMMAND, INPUT_ARG_PREFIX,
        serializedLiteEngineTaskStepInfo, TMP_PATH_ARG_PREFIX, TMP_PATH);
    if (serviceGrpcPortList != null && serviceGrpcPortList.size() > 0) {
      arg += String.format(" %s %s", GRPC_SERVICE_PORT_PREFIX, StringUtils.join(serviceGrpcPortList, " "));
    }

    List<String> args = new ArrayList<>(Collections.singletonList(arg));
    // TODO: set connector & image secret
    return CIK8ContainerParams.builder()
        .name(LITE_ENGINE_CONTAINER_NAME)
        .containerResourceParams(getLiteEngineResourceParams(stageCpuRequest, stageMemoryRequest))
        .envVars(getLiteEngineEnvVars(k8PodDetails, serviceToken, logEnvVars))
        .containerType(CIContainerType.LITE_ENGINE)
        .containerSecrets(ContainerSecrets.builder().publishArtifactConnectors(publishArtifactConnectors).build())
        .imageDetailsWithConnector(
            ImageDetailsWithConnector.builder()
                .imageDetails(ImageDetails.builder().name(LITE_ENGINE_IMAGE_NAME).tag(LITE_ENGINE_IMAGE_TAG).build())
                .imageConnectorDetails(containerImageConnectorDetails)
                .build())
        .volumeToMountPath(map)
        .commands(SH_COMMAND)
        .args(args)
        .build();
  }

  private Map<String, String> getLiteEngineEnvVars(
      K8PodDetails k8PodDetails, String serviceToken, Map<String, String> logEnvVars) {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = k8PodDetails.getBuildNumberDetails().getAccountIdentifier();
    final String projectID = k8PodDetails.getBuildNumberDetails().getProjectIdentifier();
    final String orgID = k8PodDetails.getBuildNumberDetails().getOrgIdentifier();
    final Long buildNumber = k8PodDetails.getBuildNumberDetails().getBuildNumber();
    final String stageID = k8PodDetails.getStageID();

    // Add log service environment variables
    envVars.putAll(logEnvVars);

    // Add environment variables that need to be used inside the lite engine container
    envVars.put(ENDPOINT_MINIO_VARIABLE, ENDPOINT_MINIO_VARIABLE_VALUE);
    envVars.put(BUCKET_MINIO_VARIABLE, BUCKET_MINIO_VARIABLE_VALUE);
    envVars.put(DELEGATE_SERVICE_TOKEN_VARIABLE, serviceToken);
    envVars.put(DELEGATE_SERVICE_ENDPOINT_VARIABLE, DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE);
    envVars.put(DELEGATE_SERVICE_ID_VARIABLE, DELEGATE_SERVICE_ID_VARIABLE_VALUE);
    envVars.put(LITE_ENGINE_JFROG_VARIABLE, LITE_ENGINE_JFROG_PATH);
    envVars.put(HARNESS_ACCOUNT_ID_VARIABLE, accountID);
    envVars.put(HARNESS_PROJECT_ID_VARIABLE, projectID);
    envVars.put(HARNESS_ORG_ID_VARIABLE, orgID);
    envVars.put(HARNESS_BUILD_ID_VARIABLE, buildNumber.toString());
    envVars.put(HARNESS_STAGE_ID_VARIABLE, stageID);
    return envVars;
  }

  private ContainerResourceParams getLiteEngineResourceParams(Integer stageCpuRequest, Integer stageMemoryRequest) {
    Integer cpu = stageCpuRequest + LITE_ENGINE_CONTAINER_CPU;
    Integer memory = stageMemoryRequest + LITE_ENGINE_CONTAINER_MEM;
    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(cpu)
        .resourceRequestMemoryMiB(memory)
        .resourceLimitMilliCpu(cpu)
        .resourceLimitMemoryMiB(memory)
        .build();
  }
}
