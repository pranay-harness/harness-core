package io.harness.datahandler.utils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.datahandler.models.AccountSummary;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.configuration.LimitConfigurationService;
import software.wings.beans.Account;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.verification.CVConfigurationService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

public class AccountSummaryHelperImpl implements AccountSummaryHelper {
  @Inject private LimitConfigurationService limitConfigurationService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private DelegateService delegateService;

  private AccountSummary mapToBasicAccountSummary(@NotNull Account account) {
    AccountSummary accountSummary = AccountSummary.builder()
                                        .accountId(account.getUuid())
                                        .oauthEnabled(account.isOauthEnabled())
                                        .accountName(account.getAccountName())
                                        .cloudCostEnabled(account.isCloudCostEnabled())
                                        .companyName(account.getCompanyName())
                                        .licenseInfo(account.getLicenseInfo())
                                        .twoFactorAdminEnforced(account.isTwoFactorAdminEnforced())
                                        .povEnabled(account.isPovAccount())
                                        .ceAutoCollectK8sEventsEnabled(account.isCeAutoCollectK8sEvents())
                                        .build();

    if (Objects.nonNull(account.getWhitelistedDomains())) {
      accountSummary.setWhiteListedDomains(Lists.newArrayList(account.getWhitelistedDomains()));
    } else {
      accountSummary.setWhiteListedDomains(Lists.newArrayList());
    }
    return accountSummary;
  }

  private void populateLimits(List<AccountSummary> accountSummaryList) {
    List<String> accountIds =
        accountSummaryList.stream().map(AccountSummary::getAccountId).collect(Collectors.toList());
    List<List<ConfiguredLimit>> limitsPerAccount =
        limitConfigurationService.getAllLimitsConfiguredForAccounts(accountIds);
    for (int i = 0; i < accountSummaryList.size(); i++) {
      accountSummaryList.get(i).setLimits(limitsPerAccount.get(i));
    }
  }

  private void populate24x7Guard(List<AccountSummary> accountSummaryList) {
    List<Boolean> is24x7ServiceGuardEnabledList = cvConfigurationService.is24x7GuardEnabledForAccounts(
        accountSummaryList.stream().map(AccountSummary::getAccountId).collect(Collectors.toList()));

    for (int i = 0; i < is24x7ServiceGuardEnabledList.size(); i++) {
      accountSummaryList.get(i).setIs24x7GuardEnabled(is24x7ServiceGuardEnabledList.get(i));
    }
  }

  private void populateNumberOfSecretManagers(List<AccountSummary> accountSummaryList) {
    List<Integer> numSecretManagersPerAccount = secretManagerConfigService.getCountOfSecretManagersForAccounts(
        accountSummaryList.stream().map(AccountSummary::getAccountId).collect(Collectors.toList()), true);

    for (int i = 0; i < numSecretManagersPerAccount.size(); i++) {
      accountSummaryList.get(i).setNumSecretManagers(numSecretManagersPerAccount.get(i));
    }
  }

  private void populateNumberOfDelegates(List<AccountSummary> accountSummaryList) {
    List<Integer> numberOfDelegatesPerAccount = delegateService.getCountOfDelegatesForAccounts(
        accountSummaryList.stream().map(AccountSummary::getAccountId).collect(Collectors.toList()));
    for (int i = 0; i < numberOfDelegatesPerAccount.size(); i++) {
      accountSummaryList.get(i).setNumDelegates(numberOfDelegatesPerAccount.get(i));
    }
  }

  @Override
  public List<AccountSummary> getAccountSummariesFromAccounts(List<Account> accounts) {
    List<AccountSummary> accountSummaryList =
        accounts.stream().map(this ::mapToBasicAccountSummary).collect(Collectors.toList());
    populate24x7Guard(accountSummaryList);
    populateLimits(accountSummaryList);
    populateNumberOfSecretManagers(accountSummaryList);
    populateNumberOfDelegates(accountSummaryList);
    return accountSummaryList;
  }

  @Override
  public AccountSummary getAccountSummaryFromAccount(Account account) {
    if (account == null) {
      return null;
    }
    List<AccountSummary> accountSummaryList = getAccountSummariesFromAccounts(Lists.newArrayList(account));
    if (!accountSummaryList.isEmpty()) {
      return accountSummaryList.get(0);
    }
    return null;
  }
}
