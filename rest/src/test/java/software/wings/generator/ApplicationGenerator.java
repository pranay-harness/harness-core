package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.Builder;
import software.wings.service.intfc.AppService;

@Singleton
public class ApplicationGenerator {
  @Inject AccountGenerator accountGenerator;

  @Inject AppService applicationService;

  public enum Applications {
    GENERIC_TEST,
  }

  public Application ensurePredefined(long seed, Applications predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Application ensureGenericTest(long seed) {
    return ensureApplication(seed, anApplication().withName("Test Application").build());
  }

  public Application ensureRandom(long seed) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    Applications predefined = random.nextObject(Applications.class);

    return ensurePredefined(seed, predefined);
  }

  public Application ensureApplication(long seed, Application application) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    final Builder builder = anApplication();

    if (application != null && application.getAccountId() != null) {
      builder.withAccountId(application.getAccountId());
    } else {
      Account account = accountGenerator.randomAccount();
      builder.withAccountId(account.getUuid());
    }

    if (application != null && application.getName() != null) {
      builder.withName(application.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }

    final Application newApplication = builder.build();

    final Application preexisting =
        applicationService.getAppByName(newApplication.getAccountId(), newApplication.getName());
    if (preexisting != null) {
      return preexisting;
    }

    return applicationService.save(builder.build());
  }
}
