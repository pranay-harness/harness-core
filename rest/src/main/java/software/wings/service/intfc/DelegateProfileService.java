package software.wings.service.intfc;

import software.wings.beans.DelegateProfile;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

/**
 * Created by brett on 8/4/17
 */
public interface DelegateProfileService {
  /**
   * List page response.
   *
   * @param pageRequest the page request
   * @return the page response
   */
  PageResponse<DelegateProfile> list(PageRequest<DelegateProfile> pageRequest);

  /**
   * Get delegate.
   *
   * @param accountId  the account id
   * @param delegateProfileId the delegate scope id
   * @return the delegate
   */
  DelegateProfile get(String accountId, String delegateProfileId);

  /**
   * Update delegate.
   *
   * @param delegateProfile the delegate scope
   * @return the delegate
   */
  DelegateProfile update(DelegateProfile delegateProfile);

  /**
   * Add delegate scope.
   *
   * @param delegateProfile the delegate scope
   * @return the delegate scope
   */
  DelegateProfile add(DelegateProfile delegateProfile);

  /**
   * Delete.
   *
   * @param accountId  the account id
   * @param delegateProfileId the delegate scope id
   */
  void delete(String accountId, String delegateProfileId);
}
