/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.govern.Switch.unhandled;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_COMMAND_LIBRARY_GALLERY;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;

import software.wings.beans.Account;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateGallery.TemplateGalleryBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.TemplateGalleryService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.benas.randombeans.api.EnhancedRandom;

@Singleton
public class TemplateGalleryGenerator {
  @Inject AccountGenerator accountGenerator;
  @Inject TemplateGalleryService templateGalleryService;
  @Inject WingsPersistence wingsPersistence;

  public enum TemplateGalleries { HARNESS_GALLERY, HARNESS_IMPORTED_TEMPLATE_GALLERY }

  public TemplateGallery ensurePredefined(
      Randomizer.Seed seed, OwnerManager.Owners owners, TemplateGalleries predefined) {
    switch (predefined) {
      case HARNESS_GALLERY:
        return ensureTemplateGallery(
            seed, owners, TemplateGallery.builder().name(HARNESS_GALLERY).appId(GLOBAL_APP_ID).build());
      case HARNESS_IMPORTED_TEMPLATE_GALLERY:
        return ensureTemplateGallery(seed, owners,
            TemplateGallery.builder()
                .name(HARNESS_GALLERY)
                .galleryKey(HARNESS_COMMAND_LIBRARY_GALLERY)
                .appId(GLOBAL_APP_ID)
                .build());
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
    if (templateGallery != null && templateGallery.getGalleryKey() != null) {
      builder.galleryKey(templateGallery.getGalleryKey());
    } else {
      builder.galleryKey(templateGalleryService.getAccountGalleryKey().name());
    }

    TemplateGallery existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }
    final TemplateGallery finalTemplateGallery = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> templateGalleryService.save(finalTemplateGallery), () -> exists(finalTemplateGallery));
  }

  public TemplateGallery exists(TemplateGallery templateGallery) {
    return wingsPersistence.createQuery(TemplateGallery.class)
        .filter(TemplateGallery.ACCOUNT_ID_KEY2, templateGallery.getAccountId())
        .filter(Template.NAME_KEY, templateGallery.getName())
        .get();
  }
}
