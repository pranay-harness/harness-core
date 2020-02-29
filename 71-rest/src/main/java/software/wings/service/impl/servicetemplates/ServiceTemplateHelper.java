package software.wings.service.impl.servicetemplates;

import com.google.inject.Inject;

import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;

public class ServiceTemplateHelper {
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private FeatureFlagService featureFlagService;

  public String fetchServiceTemplateId(InfrastructureMapping infrastructureMapping) {
    boolean infraRefactor =
        featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureMapping.getAccountId());
    String serviceTemplateId;
    if (infraRefactor) {
      ServiceTemplate serviceTemplate = serviceTemplateService.getOrCreate(
          infrastructureMapping.getAppId(), infrastructureMapping.getServiceId(), infrastructureMapping.getEnvId());
      if (serviceTemplate == null) {
        return null;
      }
      serviceTemplateId = serviceTemplate.getUuid();
    } else {
      serviceTemplateId = infrastructureMapping.getServiceTemplateId();
    }
    return serviceTemplateId;
  }

  public ServiceTemplate fetchServiceTemplate(InfrastructureMapping infrastructureMapping) {
    boolean infraRefactor =
        featureFlagService.isEnabled(FeatureName.INFRA_MAPPING_REFACTOR, infrastructureMapping.getAccountId());
    ServiceTemplate serviceTemplate;
    if (infraRefactor) {
      serviceTemplate = serviceTemplateService.getOrCreate(
          infrastructureMapping.getAppId(), infrastructureMapping.getServiceId(), infrastructureMapping.getEnvId());
    } else {
      serviceTemplate =
          serviceTemplateService.get(infrastructureMapping.getAppId(), infrastructureMapping.getServiceTemplateId());
    }
    return serviceTemplate;
  }
}
