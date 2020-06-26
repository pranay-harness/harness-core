package software.wings.processingcontrollers;

import static io.harness.rule.OwnerRule.MEHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.service.intfc.AccountService;

public class NotificationProcessingControllerTest extends WingsBaseTest {
  @InjectMocks @Inject private NotificationProcessingController notificationProcessingController;
  @Mock private AccountService accountService;

  private static final long ONE_DAY_TIME_DIFF = 86400000L;
  private static final long ONE_DAY_EXPIRY = System.currentTimeMillis() + ONE_DAY_TIME_DIFF;
  private static final long FOUR_DAYS_BEFORE_CURRENT_TIME = System.currentTimeMillis() - 4 * ONE_DAY_TIME_DIFF;

  private LicenseInfo getLicenseInfo(String accountStatus, long expiryTime) {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountStatus(accountStatus);
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setLicenseUnits(100);
    licenseInfo.setExpiryTime(expiryTime);
    return licenseInfo;
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldProcessActiveAccount() {
    Account account =
        anAccount().withUuid(ACCOUNT_ID).withLicenseInfo(getLicenseInfo(AccountStatus.ACTIVE, ONE_DAY_EXPIRY)).build();
    when(accountService.getFromCacheWithFallback(ACCOUNT_ID)).thenReturn(account);
    assertThat(notificationProcessingController.shouldProcessAccount(ACCOUNT_ID)).isTrue();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldNotProcessInactiveAccount() {
    Account account = anAccount()
                          .withUuid(ACCOUNT_ID)
                          .withLicenseInfo(getLicenseInfo(AccountStatus.INACTIVE, ONE_DAY_EXPIRY))
                          .build();
    when(accountService.getFromCacheWithFallback(ACCOUNT_ID)).thenReturn(account);
    assertThat(notificationProcessingController.shouldProcessAccount(ACCOUNT_ID)).isFalse();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldNotProcessDeletedAccount() {
    Account account =
        anAccount().withUuid(ACCOUNT_ID).withLicenseInfo(getLicenseInfo(AccountStatus.DELETED, ONE_DAY_EXPIRY)).build();
    when(accountService.getFromCacheWithFallback(ACCOUNT_ID)).thenReturn(account);
    assertThat(notificationProcessingController.shouldProcessAccount(ACCOUNT_ID)).isFalse();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldProcessExpiredAccountWithRecentExpiry() {
    Account account = anAccount()
                          .withUuid(ACCOUNT_ID)
                          .withLicenseInfo(getLicenseInfo(AccountStatus.EXPIRED, System.currentTimeMillis()))
                          .build();
    when(accountService.getFromCacheWithFallback(ACCOUNT_ID)).thenReturn(account);
    assertThat(notificationProcessingController.shouldProcessAccount(ACCOUNT_ID)).isTrue();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void shouldNotProcessExpiredAccountAfterFourDaysOfExpiry() {
    Account account = anAccount()
                          .withUuid(ACCOUNT_ID)
                          .withLicenseInfo(getLicenseInfo(AccountStatus.EXPIRED, FOUR_DAYS_BEFORE_CURRENT_TIME))
                          .build();
    when(accountService.getFromCacheWithFallback(ACCOUNT_ID)).thenReturn(account);
    assertThat(notificationProcessingController.shouldProcessAccount(ACCOUNT_ID)).isFalse();
  }
}