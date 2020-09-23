package software.wings.service.impl.servicetemplates;

import com.google.inject.Inject;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.ServiceTemplateService;

public class ServiceTemplateHelper {
  @Inject private ServiceTemplateService serviceTemplateService;

  public String fetchServiceTemplateId(InfrastructureMapping infrastructureMapping) {
    ServiceTemplate serviceTemplate = serviceTemplateService.getOrCreate(
        infrastructureMapping.getAppId(), infrastructureMapping.getServiceId(), infrastructureMapping.getEnvId());
    if (serviceTemplate == null) {
      return null;
    }
    return serviceTemplate.getUuid();
  }

  public ServiceTemplate fetchServiceTemplate(InfrastructureMapping infrastructureMapping) {
    return serviceTemplateService.getOrCreate(
        infrastructureMapping.getAppId(), infrastructureMapping.getServiceId(), infrastructureMapping.getEnvId());
  }
}
