package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Service.Builder.aService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.generator.ApplicationGenerator.Applications;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.ArtifactType;

@Singleton
public class ServiceGenerator {
  @Inject ApplicationGenerator applicationGenerator;

  @Inject ServiceResourceService serviceResourceService;
  @Inject WingsPersistence wingsPersistence;

  public enum Services {
    GENERIC_TEST,
  }

  public Service ensurePredefined(long seed, Services predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Service ensureGenericTest(long seed) {
    final Application application = applicationGenerator.ensurePredefined(seed, Applications.GENERIC_TEST);
    return ensureService(seed,
        aService()
            .withAppId(application.getAppId())
            .withName("Test Service")
            .withArtifactType(ArtifactType.WAR)
            .build());
  }

  public Service ensureRandom(long seed) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    Services predefined = random.nextObject(Services.class);

    return ensurePredefined(seed, predefined);
  }

  public Service exists(Service service) {
    return wingsPersistence.createQuery(Service.class)
        .filter(Service.APP_ID_KEY, service.getAppId())
        .filter(Service.NAME_KEY, service.getName())
        .get();
  }

  public Service ensureService(long seed, Service service) {
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

    Service existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    if (service != null && service.getArtifactType() != null) {
      builder.withArtifactType(service.getArtifactType());
    } else {
      builder.withArtifactType(random.nextObject(ArtifactType.class));
    }

    return serviceResourceService.save(builder.build());
  }
}
