package software.wings.scheduler;

import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.licensing.LicenseService;
import software.wings.scheduler.account.LicenseCheckHandler;
import software.wings.service.intfc.AccountService;

/**
 * @author rktummala on 11/06/2019
 */
public class LicenseCheckHandlerTest extends WingsBaseTest {
  @InjectMocks @Inject private AccountService accountService;
  @InjectMocks @Inject private LicenseService licenseService;
  @InjectMocks @Inject private LicenseCheckHandler licenseCheckHandler;

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(licenseService, "accountService", accountService, true);
    FieldUtils.writeField(accountService, "licenseService", licenseService, true);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testHandleWithValidAccountId() throws InterruptedException {
    Account account = new Account();
    account.setUuid(UUIDGenerator.generateUuid());
    account.setAccountName(ACCOUNT_NAME);
    account.setAccountKey(ACCOUNT_KEY);
    account.setCompanyName(COMPANY_NAME);
    account.setAppId(GLOBAL_APP_ID);
    account.setLicenseInfo(LicenseInfo.builder()
                               .accountType(AccountType.TRIAL)
                               .accountStatus(AccountStatus.ACTIVE)
                               .licenseUnits(100)
                               .expiryTime(System.currentTimeMillis() + 5000)
                               .build());
    Account savedAccount = accountService.save(account, false);
    Thread.sleep(10000);
    //    when(permitService.acquirePermit(any())).thenThrow(new WingsException("Exception"));
    licenseCheckHandler.handle(account);
    Account updatedAccount = accountService.get(savedAccount.getUuid());
    assertThat(updatedAccount.getLicenseInfo()).isNotNull();
    assertThat(updatedAccount.getLicenseInfo().getAccountStatus()).isEqualTo(AccountStatus.EXPIRED);
  }
}
