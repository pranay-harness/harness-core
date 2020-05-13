package io.harness.ccm.config;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.GcpServiceAccountService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import java.io.IOException;

public class CEGcpServiceAccountServiceImplTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String accountName = "ACCOUNT_NAME";
  private Account account;
  private String serviceAccountId = "harness-ce-account-name-accou"; // truncation is expected.
  private GcpServiceAccount gcpServiceAccount;

  @Mock AccountService accountService;
  @Mock GcpServiceAccountDao gcpServiceAccountDao;
  @Mock GcpServiceAccountService gcpServiceAccountService;
  @InjectMocks CEGcpServiceAccountServiceImpl ceGcpServiceAccountService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    account = new Account();
    account.setUuid(accountId);
    account.setAccountName(accountName);
    when(accountService.get(eq(accountId))).thenReturn(account);

    gcpServiceAccount = GcpServiceAccount.builder().build();
    when(gcpServiceAccountDao.getByServiceAccountId(eq(serviceAccountId))).thenReturn(gcpServiceAccount);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetDefaultServiceAccountIfAlreadyCreate() throws IOException {
    GcpServiceAccount gcpServiceAccount = ceGcpServiceAccountService.getDefaultServiceAccount(accountId);
    assertThat(gcpServiceAccount).isEqualToComparingFieldByField(this.gcpServiceAccount);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetDefaultServiceAccountIfNotYetCreated() throws IOException {
    when(gcpServiceAccountDao.getByServiceAccountId(eq(serviceAccountId)))
        .thenReturn(null)
        .thenReturn(gcpServiceAccount);
    GcpServiceAccount gcpServiceAccount = ceGcpServiceAccountService.getDefaultServiceAccount(accountId);
    assertThat(gcpServiceAccount).isEqualToComparingFieldByField(this.gcpServiceAccount);
  }
}
