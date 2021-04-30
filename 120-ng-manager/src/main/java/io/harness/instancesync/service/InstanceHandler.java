package io.harness.instancesync.service;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.instancesync.entity.DeploymentSummary;
import io.harness.instancesync.entity.deploymentinfo.OnDemandRollbackInfo;
import io.harness.instancesync.entity.infrastructureMapping.InfrastructureMapping;

import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.instance.InstanceSyncFlow;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

public abstract class InstanceHandler implements IInstanceHandler, IInstanceSyncByPerpetualTaskhandler {
  protected abstract void validateDeploymentInfo(DeploymentSummary deploymentSummary);

  protected abstract <T> Multimap<T, Instance> createDeploymentInstanceMap(DeploymentSummary deploymentSummary);

  protected abstract <T extends InfrastructureMapping> T getDeploymentInfrastructureMapping(
      DeploymentSummary deploymentSummary);

  protected abstract <T extends InfrastructureMapping, O> void syncInstancesInternal(T inframapping,
      Multimap<O, Instance> deploymentInstanceMap, DeploymentSummary newDeploymentSummary, boolean rollback,
      DelegateResponseData responseData, InstanceSyncFlow instanceSyncFlow);

  public <T, O extends InfrastructureMapping> void handleNewDeployment(
      DeploymentSummary deploymentSummary, boolean rollback, OnDemandRollbackInfo onDemandRollbackInfo) {
    if (deploymentSummary == null) {
      return;
    }

    validateDeploymentInfo(deploymentSummary);

    // Check use of generics here
    Multimap<T, Instance> deploymentToInstanceMap = createDeploymentInstanceMap(deploymentSummary);

    O inframapping = getDeploymentInfrastructureMapping(deploymentSummary);

    syncInstancesInternal(
        inframapping, deploymentToInstanceMap, deploymentSummary, rollback, null, InstanceSyncFlow.NEW_DEPLOYMENT);
  }

  public void syncInstancesInternal() {
    // fill in the deployment details VS instance map from DB
    // fetch instances from DB using inframapping id
    //        loadContainerSvcNameInstanceMap(containerInfraMapping, containerMetadataInstanceMap);
  }
}

/**
 if (isEmpty(deploymentSummaries)) {
 return;
 }

 String infraMappingId = deploymentSummaries.iterator().next().getInfraMappingId();
 String appId = deploymentSummaries.iterator().next().getAppId();
 log.info("Handling new container deployment for inframappingId [{}]", infraMappingId);
 validateDeploymentInfos(deploymentSummaries);

 if (deploymentSummaries.iterator().next().getDeploymentInfo() instanceof ContainerDeploymentInfoWithNames) {
 deploymentSummaries.forEach(deploymentSummary -> {
 ContainerDeploymentInfoWithNames deploymentInfo =
 (ContainerDeploymentInfoWithNames) deploymentSummary.getDeploymentInfo();
 containerSvcNameInstanceMap.put(ContainerMetadata.builder()
 .containerServiceName(deploymentInfo.getContainerSvcName())
 .namespace(deploymentInfo.getNamespace())
 .build(),
 null);
 });
 } else if (deploymentSummaries.iterator().next().getDeploymentInfo() instanceof K8sDeploymentInfo) {
 deploymentSummaries.forEach(deploymentSummary -> {
 K8sDeploymentInfo deploymentInfo = (K8sDeploymentInfo) deploymentSummary.getDeploymentInfo();

 String releaseName = deploymentInfo.getReleaseName();
 Set<String> namespaces = new HashSet<>();
 if (isNotBlank(deploymentInfo.getNamespace())) {
 namespaces.add(deploymentInfo.getNamespace());
 }

 if (isNotEmpty(deploymentInfo.getNamespaces())) {
 namespaces.addAll(deploymentInfo.getNamespaces());
 }

 for (String namespace : namespaces) {
 containerSvcNameInstanceMap.put(ContainerMetadata.builder()
 .type(ContainerMetadataType.K8S)
 .releaseName(releaseName)
 .namespace(namespace)
 .build(),
 null);
 }
 });
 }

 ContainerInfrastructureMapping containerInfraMapping = getContainerInfraMapping(appId, infraMappingId);
 syncInstancesInternal(
 containerInfraMapping, containerSvcNameInstanceMap, deploymentSummaries, rollback, null, NEW_DEPLOYMENT);
 *
 */