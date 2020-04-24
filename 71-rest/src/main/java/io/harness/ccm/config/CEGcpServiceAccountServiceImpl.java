package io.harness.ccm.config;

import com.google.api.services.iam.v1.model.ServiceAccount;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.GcpServiceAccountService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

@Slf4j
@Singleton
public class CEGcpServiceAccountServiceImpl implements CEGcpServiceAccountService {
  private GcpServiceAccountDao gcpServiceAccountDao;
  private GcpServiceAccountService gcpServiceAccountService;
  private AccountService accountService;

  @Inject
  public CEGcpServiceAccountServiceImpl(GcpServiceAccountDao gcpServiceAccountDao,
      GcpServiceAccountService gcpServiceAccountService, AccountService accountService) {
    this.gcpServiceAccountDao = gcpServiceAccountDao;
    this.gcpServiceAccountService = gcpServiceAccountService;
    this.accountService = accountService;
  }

  @Override
  public String create(String accountId) {
    Account account = accountService.get(accountId);
    String serviceAccountId = getServiceAccountId(account);
    String displayName = getServiceAccountDisplayName(account);

    ServiceAccount serviceAccount = gcpServiceAccountService.create(serviceAccountId, displayName);
    if (serviceAccount != null) {
      return gcpServiceAccountDao.save(GcpServiceAccount.builder()
                                           .serviceAccountId(serviceAccountId)
                                           .gcpUniqueId(serviceAccount.getUniqueId())
                                           .accountId(accountId)
                                           .email(serviceAccount.getEmail())
                                           .build());
    }
    return null;
  }

  @Override
  public GcpServiceAccount getDefaultServiceAccount(String accountId) {
    GcpServiceAccount gcpServiceAccount = get(accountId);
    if (gcpServiceAccount == null) {
      create(accountId);
      gcpServiceAccount = get(accountId);
    }
    return gcpServiceAccount;
  }

  private GcpServiceAccount get(String accountId) {
    Account account = accountService.get(accountId);
    String serviceAccountId = getServiceAccountId(account);
    return gcpServiceAccountDao.get(serviceAccountId);
  }

  private String getServiceAccountId(Account account) {
    return "harness-ce-" + getCompliedAccountName(account.getAccountName()).substring(0, 12) + "-"
        + account.getUuid().toLowerCase().substring(0, 5);
  }

  private String getServiceAccountDisplayName(Account account) {
    return "Harness CE " + account.getAccountName() + " " + account.getAccountName().substring(0, 12) + " "
        + account.getUuid();
  }

  private String getCompliedAccountName(String accountName) {
    return accountName.toLowerCase().replaceAll("[^a-z0-9]", "-");
  }
}
