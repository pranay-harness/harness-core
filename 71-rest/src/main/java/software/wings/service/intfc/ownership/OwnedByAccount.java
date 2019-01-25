package software.wings.service.intfc.ownership;

import software.wings.common.Constants;

public interface OwnedByAccount {
  String ACCOUNT_ID = Constants.ACCOUNT_ID_KEY;

  /**
   * Delete objects if they belongs to account.
   *
   * @param accountId the app id
   *
   * NOTE: The account is not an object like the others. We need to delegate the objects deletion immediately.
   */
  void deleteByAccountId(String accountId);
}
