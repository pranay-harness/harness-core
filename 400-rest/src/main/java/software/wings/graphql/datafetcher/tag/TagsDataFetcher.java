/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.tag;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.persistence.HPersistence;

import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.schema.query.QLTagsQueryParameters;
import software.wings.graphql.schema.type.QLTag;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class TagsDataFetcher extends AbstractArrayDataFetcher<QLTag, QLTagsQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  protected QLTag unusedReturnTypePassingDummyMethod() {
    return null;
  }

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  public List<QLTag> fetch(QLTagsQueryParameters qlQuery, String accountId) {
    List<HarnessTagLink> harnessTagLinks = new ArrayList<>();

    String entityId = getEntityId(qlQuery);
    if (isNotBlank(entityId)) {
      harnessTagLinks = persistence.createQuery(HarnessTagLink.class)
                            .filter(HarnessTagLinkKeys.accountId, accountId)
                            .filter(HarnessTagLinkKeys.entityId, entityId)
                            .order(HarnessTagLinkKeys.key)
                            .asList();
    }

    return harnessTagLinks.stream()
        .map(harnessTagLink -> QLTag.builder().name(harnessTagLink.getKey()).value(harnessTagLink.getValue()).build())
        .collect(Collectors.toList());
  }

  private String getEntityId(QLTagsQueryParameters qlQuery) {
    String entityId = null;

    if (isNotBlank(qlQuery.getServiceId())) {
      entityId = qlQuery.getServiceId();
    } else if (isNotBlank(qlQuery.getEnvId())) {
      entityId = qlQuery.getEnvId();
    } else if (isNotBlank(qlQuery.getWorkflowId())) {
      entityId = qlQuery.getWorkflowId();
    } else if (isNotBlank(qlQuery.getPipelineId())) {
      entityId = qlQuery.getPipelineId();
    } else if (isNotBlank(qlQuery.getTriggerId())) {
      entityId = qlQuery.getTriggerId();
    } else if (isNotBlank(qlQuery.getApplicationId())) {
      entityId = qlQuery.getApplicationId();
    }

    return entityId;
  }
}
