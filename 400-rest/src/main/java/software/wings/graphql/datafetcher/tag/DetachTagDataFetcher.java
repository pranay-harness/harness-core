package software.wings.graphql.datafetcher.tag;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.tag.QLDetachTagInput;
import software.wings.graphql.schema.mutation.tag.QLDetachTagPayload;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.HarnessTagService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class DetachTagDataFetcher extends BaseMutatorDataFetcher<QLDetachTagInput, QLDetachTagPayload> {
  private HarnessTagService harnessTagService;
  @Inject protected TagHelper tagHelper;

  @Inject
  public DetachTagDataFetcher(HarnessTagService harnessTagService) {
    super(QLDetachTagInput.class, QLDetachTagPayload.class);
    this.harnessTagService = harnessTagService;
  }

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLDetachTagPayload mutateAndFetch(QLDetachTagInput parameter, MutationContext mutationContext) {
    String appId = tagHelper.validateAndFetchAppId(parameter.getEntityId(), parameter.getEntityType());

    HarnessTagLink tagLink = HarnessTagLink.builder()
                                 .accountId(mutationContext.getAccountId())
                                 .appId(appId)
                                 .entityId(parameter.getEntityId())
                                 .entityType(EntityType.valueOf(parameter.getEntityType().name()))
                                 .key(parameter.getName())
                                 .build();

    harnessTagService.authorizeTagAttachDetach(appId, tagLink);
    harnessTagService.detachTag(tagLink);

    return QLDetachTagPayload.builder().clientMutationId(parameter.getClientMutationId()).build();
  }
}
