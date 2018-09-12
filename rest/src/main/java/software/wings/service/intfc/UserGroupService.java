package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.SSOType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rishi
 */
public interface UserGroupService {
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

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @param accountId the accountId
   * @return the userGroup
   */
  UserGroup get(@NotEmpty String accountId, @NotEmpty String uuid);

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

  /**
   * Update Overview.
   *
   * @param userGroup the userGroup
   * @return the userGroup
   */
  UserGroup updateMembers(UserGroup userGroup);

  /**
   * Remove members from the userGroup
   *
   * @return the userGroup
   */
  UserGroup removeMembers(UserGroup userGroup, Collection<User> members);

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

  boolean delete(String accountId, String userGroupId);

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
}
