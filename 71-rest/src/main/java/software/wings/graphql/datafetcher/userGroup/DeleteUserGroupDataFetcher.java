
package software.wings.graphql.datafetcher.userGroup;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLDeleteUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLDeleteStatus;
import software.wings.graphql.schema.mutation.userGroup.payload.QLDeleteUserGroupPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

@Slf4j
public class DeleteUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLDeleteUserGroupInput, QLDeleteUserGroupPayload> {
  @Inject private UserGroupService userGroupService;

  @Inject
  public DeleteUserGroupDataFetcher(UserGroupService userGroupService) {
    super(QLDeleteUserGroupInput.class, QLDeleteUserGroupPayload.class);
    this.userGroupService = userGroupService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLDeleteUserGroupPayload mutateAndFetch(QLDeleteUserGroupInput parameter, MutationContext mutationContext) {
    String userGroupId = parameter.getUserGroupId();
    UserGroup userGroup = userGroupService.get(mutationContext.getAccountId(), userGroupId);
    if (userGroup == null) {
      return QLDeleteUserGroupPayload.builder()
          .status(QLDeleteStatus.FAILED)
          .message(String.format("No userGroup exists with the id %s", userGroupId))
          .build();
    }
    userGroupService.delete(mutationContext.getAccountId(), userGroupId, false);
    return QLDeleteUserGroupPayload.builder().status(QLDeleteStatus.SUCCESS).build();
  }
}
