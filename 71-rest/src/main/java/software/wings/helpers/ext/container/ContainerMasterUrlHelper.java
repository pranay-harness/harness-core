package software.wings.helpers.ext.container;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.MasterUrlFetchTaskParameter;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.InfrastructureMappingService;

@Singleton
@Slf4j
public class ContainerMasterUrlHelper {
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject InfrastructureMappingService infrastructureMappingService;

  public boolean fetchMasterUrlAndUpdateInfraMapping(ContainerInfrastructureMapping containerInfraMapping,
      ContainerServiceParams containerServiceParams, SyncTaskContext syncTaskContext) {
    String masterUrl = fetchMasterUrl(containerInfraMapping, containerServiceParams, syncTaskContext);
    if (masterUrl != null) {
      if (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping) {
        ((GcpKubernetesInfrastructureMapping) containerInfraMapping).setMasterUrl(masterUrl);
      } else {
        ((AzureKubernetesInfrastructureMapping) containerInfraMapping).setMasterUrl(masterUrl);
      }
      InfrastructureMapping infrastructureMapping = infrastructureMappingService.save(containerInfraMapping);
      return infrastructureMapping != null;
    }
    return false;
  }

  public String fetchMasterUrl(ContainerInfrastructureMapping containerInfraMapping,
      ContainerServiceParams containerServiceParams, SyncTaskContext syncTaskContext) {
    String masterUrl = null;
    if ((containerInfraMapping instanceof GcpKubernetesInfrastructureMapping
            && ((GcpKubernetesInfrastructureMapping) containerInfraMapping).getMasterUrl() == null)
        || containerInfraMapping instanceof AzureKubernetesInfrastructureMapping
            && ((AzureKubernetesInfrastructureMapping) containerInfraMapping).getMasterUrl() == null) {
      try {
        masterUrl =
            delegateProxyFactory.get(ContainerService.class, syncTaskContext)
                .fetchMasterUrl(
                    MasterUrlFetchTaskParameter.builder().containerServiceParams(containerServiceParams).build());
      } catch (Exception e) {
        logger.warn(ExceptionUtils.getMessage(e), e);
        throw new InvalidRequestException(ExceptionUtils.getMessage(e), USER);
      }
    }
    return masterUrl;
  }

  public boolean masterUrlRequired(ContainerInfrastructureMapping containerInfraMapping) {
    return (containerInfraMapping instanceof GcpKubernetesInfrastructureMapping)
        || (containerInfraMapping instanceof AzureKubernetesInfrastructureMapping);
  }
}
