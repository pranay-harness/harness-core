/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static java.util.stream.Collectors.toList;

import io.harness.migrations.Migration;

import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTag.HarnessTagKeys;
import software.wings.dl.WingsPersistence;
import software.wings.features.TagsFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.service.intfc.HarnessTagService;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes implements Migration {
  @Inject HarnessTagService tagService;
  @Inject WingsPersistence wingsPersistence;
  @Inject @Named(TagsFeature.FEATURE_NAME) private PremiumFeature tagsFeature;

  @Override
  public void migrate() {
    log.info("Running ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes migration");

    Collection<String> accountsForWhomTagsFeatureIsNotSupported =
        getAllAccountsUsingTags()
            .stream()
            .filter(account -> !tagsFeature.isAvailableForAccount(account))
            .collect(toList());

    log.info(
        "Converting Restricted Tags To Non Restricted Tags for accounts {} ", accountsForWhomTagsFeatureIsNotSupported);

    tagService.convertRestrictedTagsToNonRestrictedTags(accountsForWhomTagsFeatureIsNotSupported);
  }

  @SuppressWarnings("unchecked")
  private Collection<String> getAllAccountsUsingTags() {
    return wingsPersistence.getCollection(HarnessTag.class).distinct(HarnessTagKeys.accountId);
  }
}
