/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.tag;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTag.HarnessTagKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLTagQueryParameters;
import software.wings.graphql.schema.type.QLTagEntity;
import software.wings.graphql.schema.type.QLTagEntity.QLTagEntityBuilder;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class TagDataFetcher extends AbstractObjectDataFetcher<QLTagEntity, QLTagQueryParameters> {
  private static final String TAG_DOES_NOT_EXIST_MSG = "Tag does not exist";
  @Inject HPersistence persistence;
  @Inject TagHelper tagHelper;

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLTagEntity fetch(QLTagQueryParameters qlQuery, String accountId) {
    HarnessTag tag = null;
    if (qlQuery.getTagId() != null) {
      tag = persistence.get(HarnessTag.class, qlQuery.getTagId());
    }
    if (qlQuery.getName() != null) {
      try (HIterator<HarnessTag> iterator = new HIterator<>(persistence.createQuery(HarnessTag.class)
                                                                .filter(HarnessTagKeys.accountId, accountId)
                                                                .filter(HarnessTagKeys.key, qlQuery.getName())
                                                                .fetch())) {
        if (iterator.hasNext()) {
          tag = iterator.next();
        }
      }
    }

    // check if accessing other account tags
    if (tag == null || (!tag.getAccountId().equals(accountId))) {
      throw new InvalidRequestException(TAG_DOES_NOT_EXIST_MSG, WingsException.USER);
    }

    final QLTagEntityBuilder builder = QLTagEntity.builder();
    tagHelper.populateTagEntity(tag, builder);
    return builder.build();
  }
}
