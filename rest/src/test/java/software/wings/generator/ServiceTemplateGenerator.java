package software.wings.generator;

import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.ServiceTemplateService;

@Singleton
public class ServiceTemplateGenerator {
  @Inject ServiceTemplateService serviceTemplateService;

  public ServiceTemplate createServiceTemplate(Randomizer.Seed seed, ServiceTemplate serviceTemplate) {
    EnhancedRandom random = Randomizer.instance(seed);

    ServiceTemplate.Builder builder = aServiceTemplate();

    if (serviceTemplate != null && serviceTemplate.getAppId() != null) {
      builder.withAppId(serviceTemplate.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (serviceTemplate != null && serviceTemplate.getEnvId() != null) {
      builder.withEnvId(serviceTemplate.getEnvId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (serviceTemplate != null && serviceTemplate.getServiceId() != null) {
      builder.withServiceId(serviceTemplate.getServiceId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (serviceTemplate != null && serviceTemplate.getName() != null) {
      builder.withName(serviceTemplate.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }

    return serviceTemplateService.save(builder.build());
  }
}
