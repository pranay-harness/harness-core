package software.wings.generator;

import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;

public class ApplicationGenerator {
  @Inject WingsPersistence wingsPersistence;

  public Application createApplication(long seed) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    final Application application = anApplication().withName(random.nextObject(String.class)).build();
    wingsPersistence.save(application);
    return application;
  }
}
