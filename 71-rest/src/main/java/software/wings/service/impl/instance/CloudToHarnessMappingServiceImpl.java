package software.wings.service.impl.instance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.service.intfc.instance.DeploymentService;

import java.util.Optional;

@Singleton
@Slf4j
public class CloudToHarnessMappingServiceImpl implements CloudToHarnessMappingService {
  private final HPersistence persistence;
  private final DeploymentService deploymentService;

  @Inject
  public CloudToHarnessMappingServiceImpl(HPersistence persistence, DeploymentService deploymentService) {
    this.persistence = persistence;
    this.deploymentService = deploymentService;
  }

  @Override
  public Optional<HarnessServiceInfo> getHarnessServiceInfo(DeploymentSummary deploymentSummary) {
    Optional<DeploymentSummary> summary = deploymentService.getWithAccountId(deploymentSummary);
    if (summary.isPresent()) {
      DeploymentSummary deploymentSummaryResponse = summary.get();
      InfrastructureMapping infrastructureMapping =
          persistence.createQuery(InfrastructureMapping.class)
              .filter(InfrastructureMappingKeys.uuid, deploymentSummaryResponse.getInfraMappingId())
              .get();

      if (infrastructureMapping != null) {
        return Optional.of(new HarnessServiceInfo(infrastructureMapping.getServiceId(),
            infrastructureMapping.getAppId(), infrastructureMapping.getComputeProviderSettingId(),
            infrastructureMapping.getEnvId(), deploymentSummaryResponse.getInfraMappingId()));
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<SettingAttribute> getSettingAttribute(String id) {
    return Optional.ofNullable(
        persistence.createQuery(SettingAttribute.class).filter(SettingAttributeKeys.uuid, id).get());
  }
}
