package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.dl.WingsPersistence;
import software.wings.generator.ApplicationGenerator.Applications;
import software.wings.generator.OwnerManager.Owners;
import software.wings.service.intfc.EnvironmentService;

@Singleton
public class EnvironmentGenerator {
  @Inject ApplicationGenerator applicationGenerator;

  @Inject EnvironmentService environmentService;
  @Inject WingsPersistence wingsPersistence;

  public enum Environments {
    GENERIC_TEST,
  }

  public Environment ensurePredefined(Randomizer.Seed seed, Owners owners, Environments predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Environment ensureGenericTest(Randomizer.Seed seed, Owners owners) {
    final Application application =
        owners.obtainApplication(() -> applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST));
    return ensureEnvironment(seed, owners,
        anEnvironment()
            .withAppId(application.getUuid())
            .withName("Test Environment")
            .withEnvironmentType(NON_PROD)
            .build());
  }

  public Environment ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    Environments predefined = random.nextObject(Environments.class);
    return ensurePredefined(seed, owners, predefined);
  }

  public Environment exists(Environment environment) {
    return wingsPersistence.createQuery(Environment.class)
        .filter(Environment.APP_ID_KEY, environment.getAppId())
        .filter(Environment.NAME_KEY, environment.getName())
        .get();
  }

  public Environment ensureEnvironment(Randomizer.Seed seed, Owners owners, Environment environment) {
    EnhancedRandom random = Randomizer.instance(seed);

    Environment.Builder builder = anEnvironment();

    if (environment != null && environment.getAppId() != null) {
      builder.withAppId(environment.getAppId());
    } else {
      Application application = owners.obtainApplication();
      if (application == null) {
        application = applicationGenerator.ensureRandom(seed, owners);
      }
      builder.withAppId(application.getUuid());
    }

    if (environment != null && environment.getName() != null) {
      builder.withName(environment.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }

    Environment existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    if (environment != null && environment.getEnvironmentType() != null) {
      builder.withEnvironmentType(environment.getEnvironmentType());
    } else {
      builder.withEnvironmentType(random.nextObject(EnvironmentType.class));
    }

    return environmentService.save(builder.build());
  }
}
