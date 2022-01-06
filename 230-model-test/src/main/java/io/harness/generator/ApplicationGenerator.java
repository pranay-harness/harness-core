/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.govern.Switch.unhandled;

import static software.wings.beans.Application.Builder.anApplication;

import io.harness.generator.AccountGenerator.Accounts;
import io.harness.generator.OwnerManager.Owners;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Application.Builder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.benas.randombeans.api.EnhancedRandom;

@Singleton
public class ApplicationGenerator {
  @Inject AccountGenerator accountGenerator;

  @Inject AppService applicationService;
  @Inject WingsPersistence wingsPersistence;

  public enum Applications { GENERIC_TEST, FUNCTIONAL_TEST }

  public Application ensurePredefined(Randomizer.Seed seed, Owners owners, Applications predefined) {
    switch (predefined) {
      case GENERIC_TEST:
        return ensureGenericTest(seed, owners);
      case FUNCTIONAL_TEST:
        return ensureFunctionalTest(seed, owners);
      default:
        unhandled(predefined);
    }

    return null;
  }

  private Application ensureGenericTest(Randomizer.Seed seed, Owners owners) {
    Account account =
        owners.obtainAccount(() -> accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST));
    return ensureApplication(seed, owners, anApplication().accountId(account.getUuid()).name("Test App").build());
  }

  private Application ensureFunctionalTest(Randomizer.Seed seed, Owners owners) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    }
    return ensureApplication(
        seed, owners, anApplication().accountId(account.getUuid()).name("Functional Test Application").build());
  }

  public Application ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    Applications predefined = random.nextObject(Applications.class);
    return ensurePredefined(seed, owners, predefined);
  }

  public Application exists(Application application) {
    return wingsPersistence.createQuery(Application.class)
        .filter(Application.ACCOUNT_ID_KEY2, application.getAccountId())
        .filter(ApplicationKeys.name, application.getName())
        .get();
  }

  public Application ensureApplication(Randomizer.Seed seed, Owners owners, Application application) {
    EnhancedRandom random = Randomizer.instance(seed);

    final Builder builder = anApplication();

    if (application != null && application.getAccountId() != null) {
      builder.accountId(application.getAccountId());
    } else {
      Account account = owners.obtainAccount(() -> accountGenerator.ensureRandom(seed, owners));
      builder.accountId(account.getUuid());
    }

    if (application != null && application.getName() != null) {
      builder.name(application.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }

    if (application != null && application.getYamlGitConfig() != null) {
      builder.yamlGitConfig(application.getYamlGitConfig());
    }

    Application existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    if (application != null && application.getCreatedBy() != null) {
      builder.createdBy(application.getCreatedBy());
    } else {
      builder.createdBy(owners.obtainUser());
    }

    final Application finalApplication = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> applicationService.save(finalApplication), () -> exists(finalApplication));
  }
}
