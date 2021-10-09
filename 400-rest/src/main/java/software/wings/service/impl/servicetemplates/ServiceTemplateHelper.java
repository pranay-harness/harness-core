package software.wings.service.impl.servicetemplates;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;

import io.harness.annotations.dev.TargetModule;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.ServiceTemplateService;

import com.google.inject.Inject;

@TargetModule(_870_CG_ORCHESTRATION)
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
