package software.wings.generator;

import static software.wings.beans.Environment.Builder.anEnvironment;

import com.google.inject.Inject;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.service.intfc.EnvironmentService;

public class EnvironmentGenerator {
  @Inject EnvironmentService environmentService;

  public Environment createEnvironment(long seed, Environment environment) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    Environment.Builder builder = anEnvironment();

    if (environment != null && environment.getAppId() != null) {
      builder.withAppId(environment.getAppId());
    } else {
      throw new UnsupportedOperationException();
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
