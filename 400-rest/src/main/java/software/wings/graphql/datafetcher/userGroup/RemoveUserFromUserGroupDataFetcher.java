/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.userGroup;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

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

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class RemoveUserFromUserGroupDataFetcher
    extends BaseMutatorDataFetcher<QLRemoveUserFromUserGroupInput, QLRemoveUserFromUserGroupPayload> {
  @Inject UserGroupService userGroupService;
  @Inject UserGroupController userGroupController;
  @Inject UserService userService;

  public RemoveUserFromUserGroupDataFetcher() {
    super(QLRemoveUserFromUserGroupInput.class, QLRemoveUserFromUserGroupPayload.class);
  }

  private UserGroup deleteUserFromUserGroup(UserGroup existingUserGroup, String userId, User user) {
    // Adding new userId to the
    List<String> memberIds = new ArrayList<>(ListUtils.emptyIfNull(existingUserGroup.getMemberIds()));
    if (!memberIds.contains(userId)) {
      throw new InvalidRequestException(String.format("No user with id %s exists in the user group", userId));
    } else {
      memberIds.remove(userId);
      existingUserGroup.setMemberIds(memberIds);
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
