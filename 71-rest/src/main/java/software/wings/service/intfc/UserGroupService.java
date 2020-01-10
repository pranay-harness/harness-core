package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.User;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.SSOType;
import software.wings.service.intfc.ownership.OwnedByAccount;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 * Created by rishi
 */
public interface UserGroupService extends OwnedByAccount {
  /**
   * Save.
   *
   * @param userGroup the userGroup
   * @return the userGroup
   */
  UserGroup save(UserGroup userGroup);

  /**
   * List page response.
   *
   * @param req the req
   * @return the page response
   */
  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserGroupService#list(software.wings.dl.PageRequest)
   */
  PageResponse<UserGroup> list(@NotEmpty String accountId, PageRequest<UserGroup> req, boolean loadUsers);

  UserGroup getUserGroupSummary(UserGroup userGroup);

  List<UserGroup> getUserGroupSummary(List<UserGroup> userGroupList);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @param accountId the accountId
   * @return the userGroup
   */
  UserGroup get(@NotEmpty String accountId, @NotEmpty String uuid);

  /**
   * Find by name.
   */
  @Nullable UserGroup getByName(@NotEmpty String accountId, @NotEmpty String name);

  /**
   * list user groups by name.
   */
  @Nullable List<UserGroup> listByName(@NotEmpty String accountId, @NotEmpty List<String> names);

  /**
   * Find by uuid with optional loadUsers flag.
   *
   * @param accountId the accountId
   * @param uuid the uuid
   * @param loadUsers populate users flag
   * @return the userGroup
   */
  UserGroup get(@NotEmpty String accountId, @NotEmpty String uuid, boolean loadUsers);

  /**
   * Update Overview.
   *
   * @param userGroup the userGroup
   * @return the userGroup
   */
  UserGroup updateOverview(UserGroup userGroup);

  UserGroup updateNotificationSettings(String accountId, String groupId, NotificationSettings newNotificationSettings);

  /**
   * Update Overview.
   *
   * @param userGroup the userGroup
   * @param sendNotification send notification flag
   * @return the userGroup
   */
  UserGroup updateMembers(UserGroup userGroup, boolean sendNotification);

  /**
   * Remove members from the userGroup
   *
   * @return the userGroup
   */
  UserGroup removeMembers(UserGroup userGroup, Collection<User> members, boolean sendNotification);

  /**
   * Update UserGroup Permissions.
   *
   * @return the userGroup
   */
  UserGroup setUserGroupPermissions(
      String accountId, String userGroupId, AccountPermissions accountPermissions, Set<AppPermission> appPermission);
  /**
   * Update Overview.
   *
   * @param userGroup the userGroup
   * @return the userGroup
   */
  UserGroup updatePermissions(UserGroup userGroup);

  /**
   * Return if there exists any user group linked to given sso provider id
   *
   * @param ssoId SSO Provider ID
   * @return
   */
  boolean existsLinkedUserGroup(@NotBlank String ssoId);

  boolean delete(String accountId, String userGroupId, boolean forceDelete);

  /**
   * Gets default "Account Administrator" user group for an account.
   *
   * @param accountId
   * @return
   */
  @Nullable UserGroup getDefaultUserGroup(String accountId);

  /**
   * Clone the given User Group with a new name
   * @param accountId The id of the account of the user group
   * @param uuid The id of the user group
   * @param newName The new name to be used for creating the cloned object
   * @param newDescription The description of the new cloned user group
   * @return The newly created clone.
   */
  UserGroup cloneUserGroup(
      @NotEmpty String accountId, @NotEmpty String uuid, @NotEmpty String newName, String newDescription);

  List<UserGroup> getUserGroupsByAccountId(String accountId, User user);

  List<UserGroup> getUserGroupsByAccountId(String accountId);

  List<String> fetchUserGroupsMemberIds(String accountId, List<String> userGroupIds);

  List<UserGroup> fetchUserGroupNamesFromIds(List<String> userGroupIds);

  boolean verifyUserAuthorizedToAcceptOrRejectApproval(String accountId, List<String> userGroupIds);

  UserGroup linkToSsoGroup(@NotBlank String accountId, @NotBlank String userGroupId, @NotNull SSOType ssoType,
      @NotBlank String ssoId, @NotBlank String ssoGroupId, @NotBlank String ssoGroupName);

  UserGroup unlinkSsoGroup(@NotBlank String accountId, @NotBlank String userGroupId, boolean retainMembers);

  /**
   * Get list of user groups linked to given sso id
   *
   * @param accountId account id
   * @param ssoId linked sso id
   * @return list of user groups
   */
  List<UserGroup> getUserGroupsBySsoId(@NotBlank String accountId, @NotBlank String ssoId);

  UserGroup fetchUserGroupByName(@NotEmpty String accountId, @NotEmpty String groupName);

  UserGroup getAdminUserGroup(String accountId);

  boolean deleteNonAdminUserGroups(String accountId);

  boolean deleteUserGroupsByName(String accountId, List<String> userGroupsToRetain);
}
