package software.wings.generator;

import static software.wings.beans.Environment.Builder.anEnvironment;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.generator.ApplicationGenerator.Applications;
import software.wings.service.intfc.EnvironmentService;

@Singleton
public class EnvironmentGenerator {
  @Inject ApplicationGenerator applicationGenerator;

  @Inject EnvironmentService environmentService;

  public Environment createEnvironment(long seed, Environment environment) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    Environment.Builder builder = anEnvironment();

    if (environment != null && environment.getAppId() != null) {
      builder.withAppId(environment.getAppId());
    } else {
      final Application application = applicationGenerator.ensurePredefined(seed, Applications.GENERIC_TEST);
      builder.withAppId(application.getUuid());
    }

    if (environment != null && environment.getName() != null) {
      builder.withName(environment.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }

    if (environment != null && environment.getEnvironmentType() != null) {
      builder.withEnvironmentType(environment.getEnvironmentType());
    } else {
      builder.withEnvironmentType(random.nextObject(EnvironmentType.class));
    }

    return environmentService.save(builder.build());
  }
}
