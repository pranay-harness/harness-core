package io.harness.stateutils.buildstate.providers;

import static io.harness.common.CIExecutionConstants.ADDON_ARGS;
import static io.harness.common.CIExecutionConstants.ADDON_CONTAINER_LIMIT_CPU;
import static io.harness.common.CIExecutionConstants.ADDON_CONTAINER_LIMIT_MEM;
import static io.harness.common.CIExecutionConstants.ADDON_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.ADDON_CONTAINER_REQ_CPU;
import static io.harness.common.CIExecutionConstants.ADDON_CONTAINER_REQ_MEM;
import static io.harness.common.CIExecutionConstants.ADDON_PATH;
import static io.harness.common.CIExecutionConstants.ADDON_PORT;
import static io.harness.common.CIExecutionConstants.ADDON_VOLUME;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_ARGS;
import static io.harness.common.CIExecutionConstants.LITE_ENGINE_CONTAINER_NAME;
import static io.harness.common.CIExecutionConstants.SH_COMMAND;
import static io.harness.govern.Switch.unhandled;
import static io.harness.stateutils.buildstate.providers.InternalImageDetailsProvider.ImageKind.ADDON_IMAGE;
import static io.harness.stateutils.buildstate.providers.InternalImageDetailsProvider.ImageKind.LITE_ENGINE_IMAGE;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;

import lombok.experimental.UtilityClass;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.ContainerResourceParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides container parameters for internally used containers
 */
@UtilityClass
// TODO: fetch constants from config file.
public class InternalContainerParamsProvider {
  public enum ContainerKind { ADDON_CONTAINER, LITE_ENGINE_CONTAINER }

  public CIK8ContainerParams getContainerParams(ContainerKind kind) {
    if (kind == null) {
      return null;
    }
    switch (kind) {
      case ADDON_CONTAINER:
        return getAddonContainerParams();
      case LITE_ENGINE_CONTAINER:
        return getLiteEngineContainerParams();
      default:
        unhandled(kind);
    }
    return null;
  }

  private CIK8ContainerParams getLiteEngineContainerParams() {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    List<String> args = new ArrayList<>(Collections.singletonList(LITE_ENGINE_ARGS));
    // todo send correct container type
    return CIK8ContainerParams.builder()
        .name(LITE_ENGINE_CONTAINER_NAME)
        .containerType(CIContainerType.LITE_ENGINE)
        .imageDetails(InternalImageDetailsProvider.getImageDetails(LITE_ENGINE_IMAGE))
        .volumeToMountPath(map)
        .commands(SH_COMMAND)
        .args(args)
        .build();
  }

  private CIK8ContainerParams getAddonContainerParams() {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    map.put(ADDON_VOLUME, ADDON_PATH);
    List<String> args = new ArrayList<>(Collections.singletonList(ADDON_ARGS));
    return CIK8ContainerParams.builder()
        .name(ADDON_CONTAINER_NAME)
        .containerResourceParams(getAddonResourceParams())
        .containerType(CIContainerType.ADD_ON)
        .imageDetails(InternalImageDetailsProvider.getImageDetails(ADDON_IMAGE))
        .volumeToMountPath(map)
        .commands(SH_COMMAND)
        .args(args)
        .ports(Collections.singletonList(ADDON_PORT))
        .build();
  }

  private ContainerResourceParams getAddonResourceParams() {
    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(ADDON_CONTAINER_REQ_CPU)
        .resourceRequestMemoryMiB(ADDON_CONTAINER_REQ_MEM)
        .resourceLimitMilliCpu(ADDON_CONTAINER_LIMIT_CPU)
        .resourceLimitMemoryMiB(ADDON_CONTAINER_LIMIT_MEM)
        .build();
  }
}
