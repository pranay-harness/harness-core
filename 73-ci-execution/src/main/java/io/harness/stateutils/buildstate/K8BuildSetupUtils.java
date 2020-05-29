package io.harness.stateutils.buildstate;

import static java.util.stream.Collectors.toList;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.common.CICommonPodConstants.STEP_EXEC_WORKING_DIR;

import com.google.inject.Singleton;

import io.harness.beans.environment.pod.PodSetupInfo;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.container.ImageDetails;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class K8BuildSetupUtils {
  // TODO Image need to be taken input from user, Have to handle password
  private ImageDetails imageDetails = ImageDetails.builder()
                                          .name("maven")
                                          .tag("3.6.3-jdk-8")
                                          .registryUrl("https://index.docker.io/v1/")
                                          .username("harshjain12")
                                          .build();

  private List<String> commands = Arrays.asList("/bin/sh", "-c");
  private List<String> args = Arrays.asList("trap : TERM INT; (while true; do sleep 1000; done) & wait");

  public CIK8PodParams<CIK8ContainerParams> getPodParams(PodSetupInfo podSetupInfo, String namespace) {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);

    List<CIK8ContainerParams> containerParams =
        podSetupInfo.getPodSetupParams()
            .getContainerDefinitionInfos()
            .stream()
            .map(containerDefinitionInfo -> {
              return CIK8ContainerParams.builder()
                  .name(containerDefinitionInfo.getName())
                  .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
                  .containerType(CIContainerType.STEP_EXECUTOR)
                  .commands(commands)
                  .args(args)
                  .imageDetails(imageDetails)
                  .volumeToMountPath(map)
                  .build();
            })
            .collect(toList());

    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(namespace)
        .stepExecVolumeName(STEP_EXEC)
        .stepExecWorkingDir(STEP_EXEC_WORKING_DIR)
        .containerParamsList(containerParams)
        .build();
  }
}
