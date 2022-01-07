/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.govern.Switch.unhandled;

import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfile.DelegateProfileKeys;
import io.harness.exception.WingsException;

import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import org.apache.commons.io.IOUtils;

@Singleton
public class DelegateProfileGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject DelegateProfileService delegateProfileService;
  @Inject private WingsPersistence wingsPersistence;

  public enum DelegateProfiles { TERRAFORM }

  public DelegateProfile ensurePredefined(
      Randomizer.Seed seed, OwnerManager.Owners owners, DelegateProfileGenerator.DelegateProfiles profile) {
    switch (profile) {
      case TERRAFORM:
        return ensureTerraform(seed, owners);
      default:
        unhandled(profile);
    }
    return null;
  }

  public void ensureAllPredefined(Randomizer.Seed seed, OwnerManager.Owners owners) {
    EnumSet.allOf(DelegateProfiles.class).forEach(predefined -> ensurePredefined(seed, owners, predefined));
  }

  private DelegateProfile ensureTerraform(Randomizer.Seed seed, OwnerManager.Owners owners) {
    DelegateProfile delegateProfile = null;
    try {
      Account account = owners.obtainAccount(() -> accountGenerator.ensureRandom(seed, owners));

      delegateProfile =
          DelegateProfile.builder().accountId(account.getUuid()).startupScript(getScript()).name("terraform").build();
    } catch (IOException exception) {
      throw new WingsException(exception);
    }
    return ensurePredefined(delegateProfile);
  }

  public DelegateProfile exists(DelegateProfile profile) {
    return wingsPersistence.createQuery(DelegateProfile.class)
        .filter(DelegateProfileKeys.accountId, profile.getAccountId())
        .filter(DelegateProfileKeys.name, profile.getName())
        .get();
  }

  public DelegateProfile ensurePredefined(DelegateProfile profile) {
    DelegateProfile existing = exists(profile);
    if (existing != null) {
      return existing;
    }

    return GeneratorUtils.suppressDuplicateException(() -> delegateProfileService.add(profile), () -> exists(profile));
  }

  private String getScript() throws IOException {
    InputStream inputStream = getClass().getResourceAsStream("/script.properties");
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
  }
}
