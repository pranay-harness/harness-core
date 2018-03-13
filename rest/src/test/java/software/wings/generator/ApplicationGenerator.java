package software.wings.generator;

import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.Application.Builder;
import software.wings.service.intfc.AppService;

public class ApplicationGenerator {
  @Inject AppService applicationService;

  public Application createApplication(long seed, Application application) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    final Builder builder = anApplication();

    if (application != null && application.getAccountId() != null) {
      builder.withAccountId(application.getAccountId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (application != null && application.getName() != null) {
      builder.withName(application.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }
    return applicationService.save(builder.build());
  }
}
