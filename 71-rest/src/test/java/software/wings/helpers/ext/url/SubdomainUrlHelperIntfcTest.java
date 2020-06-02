package software.wings.helpers.ext.url;

import static io.harness.rule.OwnerRule.MEHUL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.UrlConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AccountThreadLocal;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AccountService;

import javax.servlet.http.HttpServletRequest;

public class SubdomainUrlHelperIntfcTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private UrlConfiguration urlConfiguration;
  @InjectMocks @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;

  private static final String PORTAL_URL_WITH_SEPARATOR = PORTAL_URL + "/";
  private static final String SUBDOMAIN_URL = "https://domain.com/";
  private static final String SUBDOMAIN_URL_WITHOUT_SEPARATOR = "https://domain.com";
  private static final String API_URL = "API_URL";
  private static final String API_URL_WITH_SEPARATOR = API_URL + "/";
  private static final String DELEGATE_METADATA_URL = "http://delegate.harness.io/test/file";
  private static final String DELEGATE_METADATA_URL_WITH_SUBDOMAIN = SUBDOMAIN_URL + "test/file";
  private static final String WATCHER_METADATA_URL = "http://watcher.harness.io/test/file";
  private static final String WATCHER_METADATA_URL_WITH_SUBDOMAIN = SUBDOMAIN_URL + "test/file";
  private static final String ACCOUNT_ID_1 = "account1";
  private static final String ACCOUNT_ID_2 = "account2";
  private Account account1, account2;
  private HttpServletRequest mockHttpServletRequest;

  @Before
  public void setUp() throws Exception {
    account1 = anAccount()
                   .withCompanyName("company1")
                   .withAccountName("account1")
                   .withAccountKey("ACCOUNT_KEY")
                   .withUuid(ACCOUNT_ID_1)
                   .withLicenseInfo(getLicenseInfo())
                   .build();
    account2 = anAccount()
                   .withCompanyName("company3")
                   .withAccountName("account3")
                   .withAccountKey("ACCOUNT_KEY")
                   .withUuid(ACCOUNT_ID_2)
                   .withLicenseInfo(getLicenseInfo())
                   .withSubdomainUrl(SUBDOMAIN_URL)
                   .build();
    when(accountService.get(ACCOUNT_ID_1)).thenReturn(account1);
    when(accountService.get(ACCOUNT_ID_2)).thenReturn(account2);
    when(urlConfiguration.getPortalUrl()).thenReturn(PORTAL_URL);
    when(urlConfiguration.getApiUrl()).thenReturn(API_URL);
    when(urlConfiguration.getDelegateMetadataUrl()).thenReturn(DELEGATE_METADATA_URL);
    when(urlConfiguration.getWatcherMetadataUrl()).thenReturn(WATCHER_METADATA_URL);
    mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getScheme()).thenReturn("scheme");
    when(mockHttpServletRequest.getServerName()).thenReturn("server");
    when(mockHttpServletRequest.getServerPort()).thenReturn(0);
  }

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
  public void getPortalBaseUrlFromAccountId() {
    String result1 = subdomainUrlHelper.getPortalBaseUrl(null);
    assertThat(result1).isEqualTo(PORTAL_URL_WITH_SEPARATOR);
    String result2 = subdomainUrlHelper.getPortalBaseUrl(ACCOUNT_ID_1);
    assertThat(result2).isEqualTo(PORTAL_URL_WITH_SEPARATOR);
    String result3 = subdomainUrlHelper.getPortalBaseUrl(ACCOUNT_ID_2);
    assertThat(result3).isEqualTo(SUBDOMAIN_URL);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getPortalBaseUrlFromAccountThreadLocal() {
    AccountThreadLocal.set(ACCOUNT_ID_1);
    String result1 = subdomainUrlHelper.getPortalBaseUrl(null);
    assertThat(result1).isEqualTo(PORTAL_URL_WITH_SEPARATOR);
    AccountThreadLocal.set(ACCOUNT_ID_2);
    String result3 = subdomainUrlHelper.getPortalBaseUrl(null);
    assertThat(result3).isEqualTo(SUBDOMAIN_URL);
    AccountThreadLocal.unset();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getPortalBaseUrlFromUserThreadLocal() {
    User user1 = User.Builder.anUser()
                     .uuid("userId1")
                     .name("name1")
                     .email("user1@harness.io")
                     .defaultAccountId(ACCOUNT_ID_1)
                     .build();
    UserThreadLocal.set(user1);
    UserRequestContext userRequestContext = UserRequestContext.builder().build();
    userRequestContext.setAccountId(ACCOUNT_ID_1);
    user1.setUserRequestContext(userRequestContext);
    String result1 = subdomainUrlHelper.getPortalBaseUrl(null);
    assertThat(result1).isEqualTo(PORTAL_URL_WITH_SEPARATOR);
    userRequestContext.setAccountId(ACCOUNT_ID_2);
    user1.setUserRequestContext(userRequestContext);
    String result2 = subdomainUrlHelper.getPortalBaseUrl(null);
    assertThat(result2).isEqualTo(SUBDOMAIN_URL);
    UserThreadLocal.unset();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getApiBaseUrlFromAccountId() {
    String result1 = subdomainUrlHelper.getApiBaseUrl(null);
    assertThat(result1).isEqualTo(API_URL_WITH_SEPARATOR);
    String result2 = subdomainUrlHelper.getApiBaseUrl(ACCOUNT_ID_1);
    assertThat(result2).isEqualTo(API_URL_WITH_SEPARATOR);
    String result3 = subdomainUrlHelper.getApiBaseUrl(ACCOUNT_ID_2);
    assertThat(result3).isEqualTo(SUBDOMAIN_URL);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getApiBaseUrlFromAccountThreadLocal() {
    AccountThreadLocal.set(ACCOUNT_ID_1);
    String result1 = subdomainUrlHelper.getApiBaseUrl(null);
    assertThat(result1).isEqualTo(API_URL_WITH_SEPARATOR);
    AccountThreadLocal.set(ACCOUNT_ID_2);
    String result3 = subdomainUrlHelper.getApiBaseUrl(null);
    assertThat(result3).isEqualTo(SUBDOMAIN_URL);
    AccountThreadLocal.unset();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getApiBaseUrlFromUserThreadLocal() {
    User user1 = User.Builder.anUser()
                     .uuid("userId1")
                     .name("name1")
                     .email("user1@harness.io")
                     .defaultAccountId(ACCOUNT_ID_1)
                     .build();
    UserThreadLocal.set(user1);
    UserRequestContext userRequestContext = UserRequestContext.builder().build();
    userRequestContext.setAccountId(ACCOUNT_ID_1);
    user1.setUserRequestContext(userRequestContext);
    String result1 = subdomainUrlHelper.getApiBaseUrl(null);
    assertThat(result1).isEqualTo(API_URL_WITH_SEPARATOR);
    userRequestContext.setAccountId(ACCOUNT_ID_2);
    user1.setUserRequestContext(userRequestContext);
    String result3 = subdomainUrlHelper.getApiBaseUrl(null);
    assertThat(result3).isEqualTo(SUBDOMAIN_URL);
    UserThreadLocal.unset();
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getDelegateMetadataUrl() {
    String result1 = subdomainUrlHelper.getDelegateMetadataUrl(null);
    assertThat(result1).isEqualTo(DELEGATE_METADATA_URL);
    String result2 = subdomainUrlHelper.getDelegateMetadataUrl(ACCOUNT_ID_1);
    assertThat(result2).isEqualTo(DELEGATE_METADATA_URL);
    assertThat(subdomainUrlHelper.getDelegateMetadataUrl(ACCOUNT_ID_2)).isEqualTo(DELEGATE_METADATA_URL_WITH_SUBDOMAIN);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getWatcherMetadataUrl() {
    String result1 = subdomainUrlHelper.getWatcherMetadataUrl(null);
    assertThat(result1).isEqualTo(WATCHER_METADATA_URL);
    String result2 = subdomainUrlHelper.getWatcherMetadataUrl(ACCOUNT_ID_1);
    assertThat(result2).isEqualTo(WATCHER_METADATA_URL);
    assertThat(subdomainUrlHelper.getWatcherMetadataUrl(ACCOUNT_ID_2)).isEqualTo(WATCHER_METADATA_URL_WITH_SUBDOMAIN);
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getManagerUrl() {
    String result1 = subdomainUrlHelper.getManagerUrl(mockHttpServletRequest, null);
    assertThat(result1).isEqualTo(API_URL);
    String result2 = subdomainUrlHelper.getManagerUrl(mockHttpServletRequest, ACCOUNT_ID_1);
    assertThat(result2).isEqualTo(API_URL);
    assertThat(subdomainUrlHelper.getManagerUrl(mockHttpServletRequest, ACCOUNT_ID_2))
        .isEqualTo(SUBDOMAIN_URL_WITHOUT_SEPARATOR);
    when(urlConfiguration.getApiUrl()).thenReturn(StringUtils.EMPTY);
    String result4 = subdomainUrlHelper.getManagerUrl(mockHttpServletRequest, null);
    assertThat(result4).isEqualTo("scheme://xxxxxxxx0");
  }

  @Test
  @Owner(developers = MEHUL)
  @Category(UnitTests.class)
  public void getDownloadUrlTest() {
    assertThat(subdomainUrlHelper.getPortalBaseUrlWithoutSeparator(null)).isEqualTo(PORTAL_URL);
    assertThat(subdomainUrlHelper.getPortalBaseUrlWithoutSeparator(ACCOUNT_ID_1)).isEqualTo(PORTAL_URL);
    assertThat(subdomainUrlHelper.getPortalBaseUrlWithoutSeparator(ACCOUNT_ID_2))
        .isEqualTo(SUBDOMAIN_URL_WITHOUT_SEPARATOR);
  }
}
