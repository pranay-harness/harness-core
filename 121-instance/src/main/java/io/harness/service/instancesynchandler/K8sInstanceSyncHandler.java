package io.harness.service.instancesynchandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.yaml.InfrastructureKind;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.info.K8sServerInstanceInfo;
import io.harness.dtos.deploymentinfo.DeploymentInfoDTO;
import io.harness.dtos.deploymentinfo.K8sDeploymentInfoDTO;
import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.dtos.instanceinfo.K8sInstanceInfoDTO;
import io.harness.entities.InstanceType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.perpetualtask.PerpetualTaskType;

import com.google.inject.Singleton;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class K8sInstanceSyncHandler extends AbstractInstanceSyncHandler {
  @Override
  public String getPerpetualTaskType() {
    return PerpetualTaskType.K8S_INSTANCE_SYNC;
  }

  @Override
  public InstanceType getInstanceType() {
    return InstanceType.K8S_INSTANCE;
  }

  @Override
  public String getInfrastructureKind() {
    return InfrastructureKind.KUBERNETES_DIRECT;
  }

  @Override
  protected InstanceInfoDTO getInstanceInfoForServerInstance(ServerInstanceInfo serverInstanceInfo) {
    if (!(serverInstanceInfo instanceof K8sServerInstanceInfo)) {
      throw new InvalidArgumentsException(Pair.of("serverInstanceInfo", "Must be instance of K8sServerInstanceInfo"));
    }

    K8sServerInstanceInfo k8sServerInstanceInfo = (K8sServerInstanceInfo) serverInstanceInfo;

    return K8sInstanceInfoDTO.builder()
        .podName(k8sServerInstanceInfo.getName())
        .namespace(k8sServerInstanceInfo.getNamespace())
        .releaseName(k8sServerInstanceInfo.getReleaseName())
        .podIP(k8sServerInstanceInfo.getPodIP())
        .blueGreenColor(k8sServerInstanceInfo.getBlueGreenColor())
        .containerList(k8sServerInstanceInfo.getContainerList())
        .build();
  }

  @Override
  public DeploymentInfoDTO getDeploymentInfo(
      InfrastructureOutcome infrastructureOutcome, List<ServerInstanceInfo> serverInstanceInfoList) {
    if (isEmpty(serverInstanceInfoList)) {
      throw new InvalidArgumentsException("Parameter serverInstanceInfoList cannot be null or empty");
    }
    if (!(infrastructureOutcome instanceof K8sDirectInfrastructureOutcome)) {
      throw new InvalidArgumentsException(
          Pair.of("infrastructureOutcome", "Must be instance of K8sDirectInfrastructureOutcome"));
    }
    if (!(serverInstanceInfoList.get(0) instanceof K8sServerInstanceInfo)) {
      throw new InvalidArgumentsException(Pair.of("serverInstanceInfo", "Must be instance of K8sServerInstanceInfo"));
    }

    K8sServerInstanceInfo k8sServerInstanceInfo = (K8sServerInstanceInfo) serverInstanceInfoList.get(0);
    LinkedHashSet<String> namespaces = getNamespaces(serverInstanceInfoList);

    return K8sDeploymentInfoDTO.builder()
        .namespaces(namespaces)
        .releaseName(k8sServerInstanceInfo.getReleaseName())
        .blueGreenStageColor(k8sServerInstanceInfo.getBlueGreenColor())
        .build();
  }

  private LinkedHashSet<String> getNamespaces(@NotNull List<ServerInstanceInfo> serverInstanceInfoList) {
    return serverInstanceInfoList.stream()
        .map(K8sServerInstanceInfo.class ::cast)
        .map(K8sServerInstanceInfo.class ::cast)
        .map(K8sServerInstanceInfo::getNamespace)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
