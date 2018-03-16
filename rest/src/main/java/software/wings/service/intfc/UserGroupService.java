package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

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
  PageResponse<UserGroup> list(@NotEmpty String accountId, PageRequest<UserGroup> req);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @param accountId the accountId
   * @return the userGroup
   */
  UserGroup get(@NotEmpty String accountId, @NotEmpty String uuid);

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
   * Update Overview.
   *
   * @param userGroup the userGroup
   * @return the userGroup
   */
  UserGroup updatePermissions(UserGroup userGroup);

  boolean delete(String accountId, String userGroupId);
}
