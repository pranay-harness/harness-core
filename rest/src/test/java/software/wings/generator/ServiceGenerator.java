package software.wings.generator;

import static software.wings.beans.Service.Builder.aService;

import com.google.inject.Inject;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Service;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

public class ServiceGenerator {
  @Inject ServiceResourceService serviceResourceService;

  public Service createService(long seed, Service service) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    Service.Builder builder = aService();

    if (service != null && service.getAppId() != null) {
      builder.withAppId(service.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (service != null && service.getName() != null) {
      builder.withName(service.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }

    if (service != null && service.getArtifactType() != null) {
      builder.withArtifactType(service.getArtifactType());
    } else {
      builder.withArtifactType(random.nextObject(ArtifactType.class));
    }

    return serviceResourceService.save(builder.build());
  }
}
