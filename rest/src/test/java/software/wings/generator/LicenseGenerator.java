package software.wings.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.License.Builder.aLicense;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.License;
import software.wings.dl.WingsPersistence;

import java.util.concurrent.TimeUnit;

@Singleton
public class LicenseGenerator {
  @Inject WingsPersistence wingsPersistence;

  public enum Licenses {
    TRIAL,
  }

  public License ensurePredefined(Randomizer.Seed seed, Licenses predefined) {
    switch (predefined) {
      case TRIAL:
        return ensureTrial(seed);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private License ensureTrial(Randomizer.Seed seed) {
    License license =
        aLicense().withName("Trial").withExpiryDuration(TimeUnit.DAYS.toMillis(365)).withIsActive(true).build();
    return ensureLicense(seed, license);
  }

  public License ensureRandom(Randomizer.Seed seed) {
    EnhancedRandom random = Randomizer.instance(seed);

    Licenses predefined = random.nextObject(Licenses.class);

    return ensurePredefined(seed, predefined);
  }

  public License ensureLicense(Randomizer.Seed seed, License license) {
    EnhancedRandom random = Randomizer.instance(seed);

    final License.Builder builder = aLicense();

    if (license != null && license.getName() != null) {
      builder.withName(license.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (license != null && license.getExpiryDuration() != 0) {
      builder.withExpiryDuration(license.getExpiryDuration());
    } else {
      throw new UnsupportedOperationException();
    }

    if (license != null) {
      builder.withIsActive(license.isActive());
    }

    return wingsPersistence.saveAndGet(License.class, builder.build());
  }
}
