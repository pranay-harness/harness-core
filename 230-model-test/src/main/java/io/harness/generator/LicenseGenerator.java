/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.govern.Switch.unhandled;

import static software.wings.beans.License.Builder.aLicense;

import software.wings.beans.License;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

  public License ensureLicense(Randomizer.Seed seed, License license) {
    final License.Builder builder = aLicense();

    if (license != null && license.getName() != null) {
      builder.withName(license.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (license.getExpiryDuration() != 0) {
      builder.withExpiryDuration(license.getExpiryDuration());
    } else {
      throw new UnsupportedOperationException();
    }

    builder.withIsActive(license.isActive());

    // TODO: add ensure logic
    return wingsPersistence.saveAndGet(License.class, builder.build());
  }
}
