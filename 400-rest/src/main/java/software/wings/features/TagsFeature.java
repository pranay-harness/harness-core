/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static com.google.common.collect.Sets.newHashSet;

import software.wings.beans.EntityType;
import software.wings.beans.HarnessTag;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessTagService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@Singleton
public class TagsFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "TAGS";

  private final HarnessTagService tagService;

  @Inject
  public TagsFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, HarnessTagService tagService) {
    super(accountService, featureRestrictions);
    this.tagService = tagService;
  }

  private static boolean isRestrictedTag(HarnessTag tag) {
    return isNotEmpty(tag.getAllowedValues());
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }

    tagService.convertRestrictedTagsToNonRestrictedTags(newHashSet(accountId));

    return true;
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    return !getTags(accountId).isEmpty();
  }

  private Collection<HarnessTag> getTags(String accountId) {
    return tagService.listTags(accountId);
  }

  private Collection<HarnessTag> getRestrictedTags(String accountId) {
    return getTags(accountId).stream().filter(TagsFeature::isRestrictedTag).collect(Collectors.toList());
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    if (isAvailable(targetAccountType)) {
      return Collections.emptyList();
    }

    return getUsages(accountId);
  }

  private Collection<Usage> getUsages(String accountId) {
    return getTags(accountId).stream().map(TagsFeature::toUsage).collect(Collectors.toList());
  }

  private static Usage toUsage(HarnessTag tag) {
    return Usage.builder().entityId(tag.getUuid()).entityName(tag.getKey()).entityType(EntityType.TAG.name()).build();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }
}
