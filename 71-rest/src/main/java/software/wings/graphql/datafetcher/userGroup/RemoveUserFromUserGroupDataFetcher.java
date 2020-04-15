package software.wings.graphql.datafetcher.userGroup;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.User.Builder.anUser;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLRemoveUserFromUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLRemoveUserFromUserGroupPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RemoveUserFromUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLRemoveUserFromUserGroupInput, QLRemoveUserFromUserGroupPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;
  @Inject UserService userService;

  public RemoveUserFromUserGroupDataFetcher() {
    super(QLRemoveUserFromUserGroupInput.class, QLRemoveUserFromUserGroupPayload.class);
  }

  private User createEmptyUser(String userId) {
    return anUser().uuid(userId).build();
  }

  private UserGroup deleteUserFromUserGroup(UserGroup existingUserGroup, String userId, User user) {
    // Adding new userId to the
    List<String> memberIds = new ArrayList<>(ListUtils.emptyIfNull(existingUserGroup.getMemberIds()));
    if (!memberIds.contains(userId)) {
      throw new InvalidRequestException(String.format("No user with id %s exists in the user group", userId));
    } else {
      memberIds.remove(userId);
      existingUserGroup.setMembers(memberIds.stream().map(this ::createEmptyUser).collect(toList()));
    }
    return userGroupService.updateMembers(existingUserGroup, false, true);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLRemoveUserFromUserGroupPayload mutateAndFetch(
      QLRemoveUserFromUserGroupInput input, MutationContext mutationContext) {
    String accountId = mutationContext.getAccountId();
    // Validate that the user group exists
    String userGroupId = input.getUserGroupId();
    UserGroup userGroup = userGroupService.get(accountId, userGroupId);
    if (userGroup == null) {
      throw new InvalidRequestException(String.format("No user group exists with the id %s", userGroupId));
    }

    // Validate that the user exists
    String userId = input.getUserId();
    User user = userService.get(accountId, userId);
    if (user == null) {
      throw new InvalidRequestException(String.format("No user exists with the id %s", userId));
    }
    UserGroup updatedUserGroup = deleteUserFromUserGroup(userGroup, userId, user);
    return userGroupController.populateRemoveUserFromUserGroupPayload(updatedUserGroup, input.getClientMutationId());
  }
}
