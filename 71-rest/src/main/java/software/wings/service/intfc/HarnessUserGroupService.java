package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Account;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.security.PermissionAttribute.Action;

import java.util.List;
import java.util.Set;

/**
 * @author rktummala on 05/06/18
 */
public interface HarnessUserGroupService {
  /**
   * Save.
   *
   * @param harnessPermissions the harness permissions
   * @return the userGroup
   */
  HarnessUserGroup save(HarnessUserGroup harnessPermissions);

  /**
   * List page response.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<HarnessUserGroup> list(PageRequest<HarnessUserGroup> req);

  Set<Action> listAllowedUserActionsForAccount(@NotEmpty String accountId, String userId);

  List<Account> listAllowedSupportAccountsForUser(String userId, Set<String> excludeAccountIds);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @return the harnessPermissions
   */
  HarnessUserGroup get(@NotEmpty String uuid);

  /**
   * Update Overview.
   *
   * @param uuid the harness permissions id
   * @param accountIds the accountIds
   * @return the userGroup
   */
  HarnessUserGroup updateAccounts(@NotEmpty String uuid, Set<String> accountIds);

  /**
   * Update Overview.
   *
   * @param uuid the harness permissions id
   * @param memberIds the memberIds
   * @return the userGroup
   */
  HarnessUserGroup updateMembers(@NotEmpty String uuid, Set<String> memberIds);

  /**
   * Check if user is part of harness support.
   * @param userId
   * @return
   */
  boolean isHarnessSupportUser(String userId);

  boolean delete(@NotEmpty String uuid);
}
