package io.harness.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.harness.generator.AccountGenerator.Accounts;
import io.harness.generator.OwnerManager.Owners;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Application.Builder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;

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
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    }
    return ensureApplication(
        seed, owners, anApplication().withAccountId(account.getUuid()).withName("Test Application").build());
  }

  private Application ensureFunctionalTest(Randomizer.Seed seed, Owners owners) {
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    }
    return ensureApplication(
        seed, owners, anApplication().withAccountId(account.getUuid()).withName("Functional Test Application").build());
  }

  public Application ensureRandom(Randomizer.Seed seed, Owners owners) {
    EnhancedRandom random = Randomizer.instance(seed);
    Applications predefined = random.nextObject(Applications.class);
    return ensurePredefined(seed, owners, predefined);
  }

  public Application exists(Application application) {
    return wingsPersistence.createQuery(Application.class)
        .filter(Application.ACCOUNT_ID_KEY, application.getAccountId())
        .filter(ApplicationKeys.name, application.getName())
        .get();
  }

  public Application ensureApplication(Randomizer.Seed seed, Owners owners, Application application) {
    EnhancedRandom random = Randomizer.instance(seed);

    final Builder builder = anApplication();

    if (application != null && application.getAccountId() != null) {
      builder.withAccountId(application.getAccountId());
    } else {
      Account account = owners.obtainAccount(() -> accountGenerator.ensureRandom(seed, owners));
      builder.withAccountId(account.getUuid());
    }

    if (application != null && application.getName() != null) {
      builder.withName(application.getName());
    } else {
      builder.withName(random.nextObject(String.class));
    }

    Application existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    final Application newApplication = builder.build();

    final Application preexisting =
        applicationService.getAppByName(newApplication.getAccountId(), newApplication.getName());
    if (preexisting != null) {
      return preexisting;
    }

    final Application finalApplication = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> applicationService.save(finalApplication), () -> exists(finalApplication));
  }
}
