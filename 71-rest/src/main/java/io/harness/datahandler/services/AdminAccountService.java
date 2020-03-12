package io.harness.datahandler.services;

import io.harness.datahandler.models.AccountSummary;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.lib.Limit;
import software.wings.beans.LicenseInfo;
import software.wings.beans.LicenseUpdateInfo;

import java.util.List;

public interface AdminAccountService {
  LicenseInfo updateLicense(String accountId, LicenseUpdateInfo licenseUpdateInfo);

  List<AccountSummary> getPaginatedAccountSummaries(String offset, int pageSize);

  AccountSummary getAccountSummaryByAccountId(String accountId);

  ConfiguredLimit updateLimit(String accountId, ActionType actionType, Limit limit);

  LicenseInfo getLicense(String accountId);

  List<ConfiguredLimit> getLimitsConfiguredForAccount(String accountId);

  ConfiguredLimit getLimitConfiguredByActionType(String accountId, ActionType actionType);
}
