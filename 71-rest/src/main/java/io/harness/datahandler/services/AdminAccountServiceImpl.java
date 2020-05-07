package io.harness.datahandler.services;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.datahandler.models.AccountSummary;
import io.harness.datahandler.utils.AccountSummaryHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.Limit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Account;
import software.wings.beans.FeatureFlag;
import software.wings.beans.LicenseInfo;
import software.wings.beans.LicenseUpdateInfo;
import software.wings.beans.User;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UserService;

import java.util.List;
import java.util.Objects;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class AdminAccountServiceImpl implements AdminAccountService {
  @Inject private AccountService accountService;
  @Inject private UserService userService;
  @Inject private LicenseService licenseService;
  @Inject private LimitConfigurationService limitConfigurationService;
  @Inject private AccountSummaryHelper accountSummaryHelper;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public LicenseInfo updateLicense(String accountId, LicenseUpdateInfo licenseUpdateInfo) {
    boolean success = licenseService.updateAccountLicense(accountId, licenseUpdateInfo.getLicenseInfo());
    if (success) {
      return getLicense(accountId);
    }
    return null;
  }

  @Override
  public List<AccountSummary> getPaginatedAccountSummaries(String offset, int pageSize) {
    PageRequest<Account> accountPageRequest =
        aPageRequest().withOffset(offset).withLimit(String.valueOf(pageSize)).build();
    List<Account> accountList = accountService.getAccounts(accountPageRequest);
    return accountSummaryHelper.getAccountSummariesFromAccounts(accountList);
  }

  @Override
  public AccountSummary getAccountSummaryByAccountId(String accountId) {
    Account account = accountService.get(accountId);
    return accountSummaryHelper.getAccountSummaryFromAccount(account);
  }

  @Override
  public ConfiguredLimit updateLimit(String accountId, ActionType actionType, Limit limit) {
    boolean success = limitConfigurationService.configure(accountId, actionType, limit);
    if (success) {
      return limitConfigurationService.get(accountId, actionType);
    }
    return null;
  }

  @Override
  public LicenseInfo getLicense(String accountId) {
    Account account = accountService.get(accountId);
    if (account != null) {
      return account.getLicenseInfo();
    }
    return null;
  }

  @Override
  public List<ConfiguredLimit> getLimitsConfiguredForAccount(String accountId) {
    return limitConfigurationService.getLimitsConfiguredForAccount(accountId);
  }

  @Override
  public ConfiguredLimit getLimitConfiguredByActionType(String accountId, ActionType actionType) {
    return limitConfigurationService.get(accountId, actionType);
  }

  @Override
  public Account createAccount(Account account, String adminUserEmail) {
    User user = null;
    boolean addUser = false;
    if (!StringUtils.isEmpty(adminUserEmail)) {
      user = userService.getUserByEmail(adminUserEmail);
      if (Objects.isNull(user)) {
        throw new InvalidRequestException("User does not exist in the system.");
      }
      addUser = true;
    }
    return userService.addAccount(account, user, addUser);
  }

  @Override
  public boolean enableAccount(String accountId) {
    return accountService.enableAccount(accountId);
  }

  @Override
  public boolean disableAccount(String accountId, String newClusterUrl) {
    return accountService.disableAccount(accountId, newClusterUrl);
  }

  @Override
  public boolean enableOrDisableCloudCost(String accountId, boolean enabled) {
    return accountService.updateCloudCostEnabled(accountId, enabled);
  }

  @Override
  public boolean enableOrDisableCeK8sEventCollection(String accountId, boolean ceK8sEventCollectionEnabled) {
    return accountService.updateCeAutoCollectK8sEvents(accountId, ceK8sEventCollectionEnabled);
  }

  @Override
  public boolean delete(String accountId) {
    return accountService.delete(accountId);
  }

  @Override
  public FeatureFlag updateFeatureFlagForAccount(String accountId, String featureName, boolean enabled) {
    return featureFlagService.updateFeatureFlagForAccount(featureName, accountId, enabled);
  }
}
