package software.wings.graphql.datafetcher.userGroup;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLCreateUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLCreateUserGroupPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

@Slf4j
public class CreateUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLCreateUserGroupInput, QLCreateUserGroupPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;

  @Inject
  public CreateUserGroupDataFetcher(UserGroupService userGroupService) {
    super(QLCreateUserGroupInput.class, QLCreateUserGroupPayload.class);
    this.userGroupService = userGroupService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLCreateUserGroupPayload mutateAndFetch(QLCreateUserGroupInput parameter, MutationContext mutationContext) {
    UserGroup userGroup = userGroupController.populateUserGroupEntity(parameter);
    userGroup.setAccountId(mutationContext.getAccountId());
    userGroupService.save(userGroup);
    return userGroupController.populateCreateUserGroupPayload(userGroup, parameter.getRequestId());
  }
}
