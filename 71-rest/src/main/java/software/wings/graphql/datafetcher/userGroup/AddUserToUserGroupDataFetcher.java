package software.wings.graphql.datafetcher.userGroup;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.User.Builder.anUser;

import com.google.inject.Inject;

import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.userGroup.input.QLAddUserToUserGroupInput;
import software.wings.graphql.schema.mutation.userGroup.payload.QLAddUserToUserGroupPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AddUserToUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLAddUserToUserGroupInput, QLAddUserToUserGroupPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;
  @Inject UserService userService;

  public AddUserToUserGroupDataFetcher() {
    super(QLAddUserToUserGroupInput.class, QLAddUserToUserGroupPayload.class);
  }

  private User createEmptyUser(String userId) {
    return anUser().uuid(userId).build();
  }

  private UserGroup addUserToUserGroup(UserGroup existingUserGroup, String userId, User user) {
    boolean sendNotification = true;
    if (existingUserGroup.getNotificationSettings() != null) {
      sendNotification = existingUserGroup.getNotificationSettings().isSendMailToNewMembers();
    }
    // Adding new userId to the
    List<String> memberIds = new ArrayList<>(ListUtils.emptyIfNull(existingUserGroup.getMemberIds()));
    if (memberIds.contains(userId)) {
      throw new DuplicateFieldException(String.format("A user with id %s already exists in the user group", userId));
    } else {
      memberIds.add(userId);
      existingUserGroup.setMembers(memberIds.stream().map(this ::createEmptyUser).collect(toList()));
    }
    return userGroupService.updateMembers(existingUserGroup, sendNotification, true);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLAddUserToUserGroupPayload mutateAndFetch(
      QLAddUserToUserGroupInput input, MutationContext mutationContext) {
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
    UserGroup updatedUserGroup = addUserToUserGroup(userGroup, userId, user);
    return userGroupController.populateAddUserToUserGroupPayload(updatedUserGroup, input.getClientMutationId());
  }
}
