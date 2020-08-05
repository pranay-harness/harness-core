package software.wings.features.api;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.service.intfc.AccountService;

@Slf4j
public abstract class AbstractRestrictedCeFeature extends AbstractRestrictedFeature {
  public AbstractRestrictedCeFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  protected String getAccountType(String accountId) {
    String accountType = accountService.getCeAccountType(accountId).orElse(AccountType.PAID);
    logger.info("Account type: {}", accountType);
    return accountService.getCeAccountType(accountId).orElse(AccountType.PAID);
  }

  @Override
  public Restrictions getRestrictionsForAccount(String accountId) {
    return getRestrictions(getAccountType(accountId));
  }
}
