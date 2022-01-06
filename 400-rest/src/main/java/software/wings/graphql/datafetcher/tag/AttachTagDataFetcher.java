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

import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.tag.QLAttachTagInput;
import software.wings.graphql.schema.mutation.tag.QLAttachTagPayload;
import software.wings.graphql.schema.type.QLTagLink;
import software.wings.graphql.schema.type.aggregation.QLEntityType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.HarnessTagService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class AttachTagDataFetcher extends BaseMutatorDataFetcher<QLAttachTagInput, QLAttachTagPayload> {
  private HarnessTagService harnessTagService;
  @Inject protected TagHelper tagHelper;

  @Inject
  public AttachTagDataFetcher(HarnessTagService harnessTagService) {
    super(QLAttachTagInput.class, QLAttachTagPayload.class);
    this.harnessTagService = harnessTagService;
  }

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLAttachTagPayload mutateAndFetch(QLAttachTagInput parameter, MutationContext mutationContext) {
    String appId = tagHelper.validateAndFetchAppId(parameter.getEntityId(), parameter.getEntityType());

    HarnessTagLink tagLink = HarnessTagLink.builder()
                                 .accountId(mutationContext.getAccountId())
                                 .appId(appId)
                                 .entityId(parameter.getEntityId())
                                 .entityType(EntityType.valueOf(parameter.getEntityType().name()))
                                 .key(parameter.getName())
                                 .value(parameter.getValue() == null ? "" : parameter.getValue())
                                 .build();

    harnessTagService.authorizeTagAttachDetach(appId, tagLink);
    harnessTagService.attachTag(tagLink);

    return QLAttachTagPayload.builder()
        .clientMutationId(parameter.getClientMutationId())
        .tagLink(QLTagLink.builder()
                     .name(tagLink.getKey())
                     .value(tagLink.getValue())
                     .entityId(tagLink.getEntityId())
                     .entityType(QLEntityType.valueOf(tagLink.getEntityType().name()))
                     .appId(tagLink.getAppId())
                     .build())
        .build();
  }
}
