package io.harness.generator;

import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateGallery.TemplateGalleryBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

@Singleton
public class TemplateGalleryGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject TemplateGalleryService templateGalleryService;
  @Inject WingsPersistence wingsPersistence;
  public enum TemplateGalleries { HARNESS_GALLERY }

  public TemplateGallery ensurePredefined(
      Randomizer.Seed seed, OwnerManager.Owners owners, TemplateGalleries predefined) {
    switch (predefined) {
      case HARNESS_GALLERY:
        return ensureTemplateGallery(
            seed, owners, TemplateGallery.builder().name(HARNESS_GALLERY).appId(GLOBAL_APP_ID).build());
      default:
        unhandled(predefined);
    }
    return null;
  }

  private TemplateGallery ensureTemplateGallery(
      Randomizer.Seed seed, OwnerManager.Owners owners, TemplateGallery templateGallery) {
    EnhancedRandom random = Randomizer.instance(seed);
    Account account = owners.obtainAccount();
    if (account == null) {
      account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    }

    TemplateGalleryBuilder builder = TemplateGallery.builder();
    if (templateGallery != null && templateGallery.getAccountId() != null) {
      builder.accountId(templateGallery.getAccountId());
    } else {
      builder.accountId(account.getUuid());
    }

    if (templateGallery != null && templateGallery.getName() != null) {
      builder.name(templateGallery.getName());
    } else {
      builder.name(random.nextObject(String.class));
    }

    TemplateGallery globalHarnessGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
    if (globalHarnessGallery != null) {
      builder.referencedGalleryId(globalHarnessGallery.getUuid());
    }

    builder.appId(GLOBAL_APP_ID);

    TemplateGallery existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }
    return templateGalleryService.save(builder.build());
  }

  public TemplateGallery exists(TemplateGallery templateGallery) {
    return wingsPersistence.createQuery(TemplateGallery.class)
        .filter(TemplateGallery.ACCOUNT_ID_KEY, templateGallery.getAccountId())
        .filter(Template.NAME_KEY, templateGallery.getName())
        .get();
  }
}
