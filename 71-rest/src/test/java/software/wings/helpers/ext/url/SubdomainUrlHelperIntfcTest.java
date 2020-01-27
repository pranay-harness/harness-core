package software.wings.helpers.ext.url;

import static io.harness.rule.OwnerRule.MEHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;

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
import software.wings.beans.User;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubdomainUrlHelperIntfcTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @InjectMocks @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;

  private static final String PORTAL_URL_TO_VERIFY = PORTAL_URL + "/";
  private static final String SUBDOMAIN_URL1 = "https://domain.com/";
  private static final String API_URL_TO_VERIFY = "http:localhost:8080/";

  protected LicenseInfo getLicenseInfo() {
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setLicenseUnits(100);
    licenseInfo.setExpireAfterDays(15);
    return licenseInfo;
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getPortalBaseUrlTest() {
    String result1 = subdomainUrlHelper.getPortalBaseUrl(Optional.ofNullable(null));
    assertThat(result1).isEqualTo(PORTAL_URL_TO_VERIFY);
    String result2 = subdomainUrlHelper.getPortalBaseUrl(Optional.ofNullable(SUBDOMAIN_URL1));
    assertThat(result2).isEqualTo(SUBDOMAIN_URL1);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getApiBaseUrlTest() {
    String result1 = subdomainUrlHelper.getApiBaseUrl(Optional.ofNullable(null));
    assertThat(result1).isEqualTo(API_URL_TO_VERIFY);
    String result2 = subdomainUrlHelper.getApiBaseUrl(Optional.ofNullable(SUBDOMAIN_URL1));
    assertThat(result2).isEqualTo(SUBDOMAIN_URL1);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getAPIUrlTest() {
    assertThat(subdomainUrlHelper.getAPIUrl()).isEqualTo(API_URL_TO_VERIFY);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getCustomSubDomainUrlTest() {
    Account account1 = anAccount()
                           .withCompanyName("company1")
                           .withAccountName("account1")
                           .withAccountKey("ACCOUNT_KEY")
                           .withUuid("account1")
                           .withLicenseInfo(getLicenseInfo())
                           .build();
    when(accountService.get(account1.getUuid())).thenReturn(account1);
    Optional<String> result1 = subdomainUrlHelper.getCustomSubDomainUrl(Optional.ofNullable(account1.getUuid()));
    assertThat(result1.isPresent()).isFalse();
    Account account2 = anAccount()
                           .withCompanyName("company2")
                           .withAccountName("account2")
                           .withAccountKey("ACCOUNT_KEY")
                           .withUuid("account2")
                           .withLicenseInfo(getLicenseInfo())
                           .withSubdomainUrl(SUBDOMAIN_URL1)
                           .build();
    when(accountService.get(account2.getUuid())).thenReturn(account2);
    Optional<String> result2 = subdomainUrlHelper.getCustomSubDomainUrl(Optional.ofNullable(account2.getUuid()));
    assertThat(result2.isPresent()).isTrue();
    assertThat(result2.get()).isEqualTo(SUBDOMAIN_URL1);
    User user1 = User.Builder.anUser().uuid("userId1").name("name1").email("user1@harness.io").build();
    List<Account> accountList = new ArrayList<>();
    accountList.add(account1);
    user1.setAccounts(accountList);
    UserThreadLocal.set(user1);
    UserRequestContext userRequestContext1 = UserRequestContext.builder().build();
    userRequestContext1.setAccountId(account1.getUuid());
    user1.setUserRequestContext(userRequestContext1);
    Optional<String> result3 = subdomainUrlHelper.getCustomSubDomainUrl(Optional.ofNullable(null));
    assertThat(result3.isPresent()).isFalse();
    accountList.remove(account1);
    accountList.add(account2);
    user1.setAccounts(accountList);
    userRequestContext1.setAccountId(account2.getUuid());
    user1.setUserRequestContext(userRequestContext1);
    Optional<String> result4 = subdomainUrlHelper.getCustomSubDomainUrl(Optional.ofNullable(null));
    assertThat(result4.isPresent()).isTrue();
    assertThat(result4.get()).isEqualTo(SUBDOMAIN_URL1);
    UserThreadLocal.unset();
  }
}
