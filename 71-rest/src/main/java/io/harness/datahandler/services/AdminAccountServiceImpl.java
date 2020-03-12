package io.harness.datahandler.services;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.datahandler.models.AccountSummary;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import io.harness.limits.lib.Limit;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;
import software.wings.beans.LicenseUpdateInfo;
import software.wings.licensing.LicenseService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.verification.CVConfigurationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class AdminAccountServiceImpl implements AdminAccountService {
  @Inject private AccountService accountService;
  @Inject private UserService userService;
  @Inject private LicenseService licenseService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private LimitConfigurationService limitConfigurationService;

  @Override
  public LicenseInfo updateLicense(String accountId, LicenseUpdateInfo licenseUpdateInfo) {
    boolean success = licenseService.updateAccountLicense(accountId, licenseUpdateInfo.getLicenseInfo());
    if (success) {
      return getLicense(accountId);
    }
    return null;
  }

  private AccountSummary getAccountSummaryFromAccount(@NotNull Account account) {
    AccountSummary accountSummary = AccountSummary.builder()
                                        .accountId(account.getUuid())
                                        .oauthEnabled(account.isOauthEnabled())
                                        .accountName(account.getAccountName())
                                        .cloudCostEnabled(account.isCloudCostEnabled())
                                        .companyName(account.getCompanyName())
                                        .licenseInfo(account.getLicenseInfo())
                                        .twoFactorAdminEnforced(account.isTwoFactorAdminEnforced())
                                        .build();

    if (Objects.nonNull(account.getWhitelistedDomains())) {
      accountSummary.setWhiteListedDomains(Lists.newArrayList(account.getWhitelistedDomains()));
    } else {
      accountSummary.setWhiteListedDomains(Lists.newArrayList());
    }

    List<ConfiguredLimit> limits = limitConfigurationService.getAllLimitsConfiguredForAccount(account.getUuid());
    if (Objects.nonNull(limits)) {
      accountSummary.setLimits(limits);
    } else {
      accountSummary.setLimits(new ArrayList<>());
    }

    accountSummary.setIs24x7GuardEnabled(cvConfigurationService.is24x7GuardEnabledForAccount(account.getUuid()));
    return accountSummary;
  }

  @Override
  public List<AccountSummary> getPaginatedAccountSummaries(String offset, int pageSize) {
    PageRequest<Account> accountPageRequest =
        aPageRequest().withOffset(offset).withLimit(String.valueOf(pageSize)).build();
    List<Account> accountList = accountService.getAccounts(accountPageRequest);
    return accountList.stream().map(this ::getAccountSummaryFromAccount).collect(Collectors.toList());
  }

  @Override
  public AccountSummary getAccountSummaryByAccountId(String accountId) {
    return getAccountSummaryFromAccount(accountService.get(accountId));
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
    return limitConfigurationService.getAllLimitsConfiguredForAccount(accountId);
  }

  @Override
  public ConfiguredLimit getLimitConfiguredByActionType(String accountId, ActionType actionType) {
    return limitConfigurationService.get(accountId, actionType);
  }
}
