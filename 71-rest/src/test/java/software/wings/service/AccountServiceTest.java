package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.COMPANY_NAME;
import static software.wings.utils.WingsTestConstants.ILLEGAL_ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.PORTAL_URL;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.beans.StringValue.Builder;
import software.wings.beans.TechStack;
import software.wings.beans.UrlInfo;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.licensing.LicenseService;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
public class AccountServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private UserService userService;
  @Mock private SettingsService settingsService;
  @Mock private TemplateGalleryService templateGalleryService;
  @Mock private UserPermissionInfo mockUserPermissionInfo;
  @Mock private AuthService authService;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;

  @InjectMocks @Inject private LicenseService licenseService;
  @InjectMocks @Inject private AccountService accountService;

  @Inject private WingsPersistence wingsPersistence;

  @Rule public ExpectedException thrown = ExpectedException.none();
  private static final String HARNESS_NAME = "Harness";
  private final String serviceId = UUID.randomUUID().toString();
  private final String envId = UUID.randomUUID().toString();
  private final String accountId = UUID.randomUUID().toString();
  private final String appId = UUID.randomUUID().toString();
  private final String workflowId = UUID.randomUUID().toString();
  private final String cvConfigId = UUID.randomUUID().toString();
  private final User user = new User();

  @Before
  public void setup() throws IllegalAccessException {
    FieldUtils.writeField(licenseService, "accountService", accountService, true);
    FieldUtils.writeField(accountService, "licenseService", licenseService, true);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSaveAccount() {
    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(getLicenseInfo())
                                              .build());
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
  }

  private Account getAccount() {
    Map<String, UrlInfo> techStacksLinkMap = new HashMap<>();
    techStacksLinkMap.put("Deployment-AWS",
        UrlInfo.builder()
            .title("deployment-aws")
            .url("https://docs.harness.io/article/whwnovprrb-cloud-providers#amazon_web_services_aws_cloud")
            .build());
    techStacksLinkMap.put("Deployment-General",
        UrlInfo.builder()
            .title("deployment-general")
            .url("https://docs.harness.io/article/whwnovprrb-cloud-providers")
            .build());
    techStacksLinkMap.put("Artifact-General",
        UrlInfo.builder()
            .title("artifact-general")
            .url("https://docs.harness.io/article/7dghbx1dbl-configuring-artifact-server")
            .build());
    techStacksLinkMap.put("Monitoring-General",
        UrlInfo.builder()
            .title("monitoring-general")
            .url("https://docs.harness.io/article/r6ut6tldy0-verification-providers")
            .build());
    when(configuration.getTechStackLinks()).thenReturn(techStacksLinkMap);
    when(userService.getUsersOfAccount(any()))
        .thenReturn(Arrays.asList(
            User.Builder.anUser().withUuid("userId1").withName("name1").withEmail("user1@harness.io").build()));
    return accountService.save(anAccount()
                                   .withCompanyName(HARNESS_NAME)
                                   .withAccountName(HARNESS_NAME)
                                   .withAccountKey("ACCOUNT_KEY")
                                   .withLicenseInfo(getLicenseInfo())
                                   .build());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateTechStacks() {
    Account account = getAccount();
    Set<TechStack> techStackSet = new HashSet<>();
    TechStack techStack = TechStack.builder().category("Deployment Platforms").technology("AWS").build();
    techStackSet.add(techStack);
    boolean success = accountService.updateTechStacks(account.getUuid(), techStackSet);
    assertThat(success).isTrue();
    ArgumentCaptor<EmailData> captor = ArgumentCaptor.forClass(EmailData.class);
    verify(emailNotificationService).send(captor.capture());
    EmailData emailData = captor.getValue();
    assertThat(emailData.getTo()).hasSize(1);
    assertThat(emailData.getTo()).contains("user1@harness.io");

    Account accountAfterUpdate = accountService.get(account.getUuid());
    assertThat(accountAfterUpdate.getTechStacks()).hasSize(1);
    TechStack techStack1 = accountAfterUpdate.getTechStacks().toArray(new TechStack[0])[0];
    assertThat(techStack1.getCategory()).isEqualTo("Deployment Platforms");
    assertThat(techStack1.getTechnology()).isEqualTo("AWS");
    Map<String, Object> templateModel = (Map<String, Object>) emailData.getTemplateModel();
    List<String> techStackLinks = (List<String>) templateModel.get("deploymentPlatforms");
    assertThat(techStackLinks).hasSize(1);
    String link = techStackLinks.get(0);
    assertThat(link).endsWith(
        "https://docs.harness.io/article/whwnovprrb-cloud-providers#amazon_web_services_aws_cloud");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateTechStacksWithNullTechStack() {
    Account account = getAccount();
    boolean success = accountService.updateTechStacks(account.getUuid(), null);
    assertThat(success).isTrue();
    ArgumentCaptor<EmailData> captor = ArgumentCaptor.forClass(EmailData.class);
    verify(emailNotificationService).send(captor.capture());
    EmailData emailData = captor.getValue();
    assertThat(emailData.getTo()).hasSize(1);
    assertThat(emailData.getTo()).contains("user1@harness.io");
    Map<String, Object> templateModel = (Map<String, Object>) emailData.getTemplateModel();
    List<String> techStackLinks = (List<String>) templateModel.get("deploymentPlatforms");
    assertThat(techStackLinks).hasSize(1);
    String link = techStackLinks.get(0);
    assertThat(link).endsWith("https://docs.harness.io/article/whwnovprrb-cloud-providers");
  }

  @Test
  @Category(UnitTests.class)
  public void testGetAccountType() {
    LicenseInfo licenseInfo = getLicenseInfo();
    licenseInfo.setAccountType(AccountType.COMMUNITY);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());

    assertThat(accountService.getAccountType(account.getUuid())).isEqualTo(Optional.of(AccountType.COMMUNITY));
  }

  @Test
  @Category(UnitTests.class)
  public void testRegisterNewUser_invalidAccountName_shouldFail() {
    Account account = anAccount()
                          .withCompanyName(COMPANY_NAME)
                          .withAccountName(ILLEGAL_ACCOUNT_NAME)
                          .withLicenseInfo(getLicenseInfo())
                          .build();

    when(configuration.getPortal().getUrl()).thenReturn(PORTAL_URL);

    try {
      accountService.save(account);
      fail("Exception is expected when inviting with invalid account name");
    } catch (WingsException e) {
      // Ignore, exception expected here.
    }
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFailSavingAccountWithoutLicense() {
    thrown.expect(WingsException.class);
    thrown.expectMessage("Invalid / Null license info");
    accountService.save(
        anAccount().withCompanyName(HARNESS_NAME).withAccountName(HARNESS_NAME).withAccountKey("ACCOUNT_KEY").build());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteAccount() {
    String accountId = wingsPersistence.save(anAccount().withCompanyName(HARNESS_NAME).build());
    accountService.delete(accountId);
    assertThat(wingsPersistence.get(Account.class, accountId)).isNull();
    verify(appService).deleteByAccountId(accountId);
    verify(settingsService).deleteByAccountId(accountId);
    verify(templateGalleryService).deleteByAccountId(accountId);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldUpdateCompanyName() {
    Account account = wingsPersistence.saveAndGet(Account.class,
        anAccount()
            .withCompanyName("Wings")
            .withAccountName("Wings")
            .withWhitelistedDomains(Collections.singleton("mike@harness.io"))
            .build());
    account.setCompanyName(HARNESS_NAME);
    accountService.update(account);
    assertThat(wingsPersistence.get(Account.class, account.getUuid())).isEqualTo(account);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetAccountByCompanyName() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.getByName(HARNESS_NAME)).isEqualTo(account);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetAccountByAccountName() {
    Account account = wingsPersistence.saveAndGet(
        Account.class, anAccount().withAccountName(HARNESS_NAME).withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.getByAccountName(HARNESS_NAME)).isEqualTo(account);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetAccount() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDelegateConfiguration() {
    String accountId =
        wingsPersistence.save(anAccount()
                                  .withCompanyName(HARNESS_NAME)
                                  .withDelegateConfiguration(DelegateConfiguration.builder()
                                                                 .watcherVersion("1.0.1")
                                                                 .delegateVersions(asList("1.0.0", "1.0.1"))
                                                                 .build())
                                  .build());
    assertThat(accountService.getDelegateConfiguration(accountId))
        .hasFieldOrPropertyWithValue("watcherVersion", "1.0.1")
        .hasFieldOrPropertyWithValue("delegateVersions", asList("1.0.0", "1.0.1"));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetDelegateConfigurationFromGlobalAccount() {
    wingsPersistence.save(anAccount()
                              .withUuid(GLOBAL_ACCOUNT_ID)
                              .withCompanyName(HARNESS_NAME)
                              .withDelegateConfiguration(DelegateConfiguration.builder()
                                                             .watcherVersion("globalVersion")
                                                             .delegateVersions(asList("globalVersion"))
                                                             .build())
                              .build());

    String accountId = wingsPersistence.save(anAccount().withCompanyName(HARNESS_NAME).build());

    assertThat(accountService.getDelegateConfiguration(accountId))
        .hasFieldOrPropertyWithValue("watcherVersion", "globalVersion")
        .hasFieldOrPropertyWithValue("delegateVersions", asList("globalVersion"));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldListAllAccounts() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(accountService.get(account.getUuid())).isEqualTo(account);
    assertThat(accountService.listAllAccounts()).isNotEmpty();
    assertThat(accountService.listAccounts(Collections.emptySet())).isNotEmpty();
    assertThat(accountService.listAllAccounts().get(0)).isNotNull();
    assertThat(accountService.listAllAccountWithDefaultsWithoutLicenseInfo()).isNotEmpty();
    assertThat(accountService.listAllAccountWithDefaultsWithoutLicenseInfo().get(0)).isNotNull();
    assertThat(accountService.listAllAccountWithDefaultsWithoutLicenseInfo().get(0).getUuid())
        .isEqualTo(account.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldGetAccountWithDefaults() {
    Account account = wingsPersistence.saveAndGet(Account.class, anAccount().withCompanyName(HARNESS_NAME).build());
    assertThat(account).isNotNull();

    List<SettingAttribute> settingAttributes = asList(aSettingAttribute()
                                                          .withName("NAME")
                                                          .withAccountId(account.getUuid())
                                                          .withValue(Builder.aStringValue().build())
                                                          .build(),
        aSettingAttribute()
            .withName("NAME2")
            .withAccountId(account.getUuid())
            .withValue(Builder.aStringValue().withValue("VALUE").build())
            .build());

    when(settingsService.listAccountDefaults(account.getUuid()))
        .thenReturn(settingAttributes.stream().collect(Collectors.toMap(SettingAttribute::getName,
            settingAttribute
            -> Optional.ofNullable(((StringValue) settingAttribute.getValue()).getValue()).orElse(""),
            (a, b) -> b)));

    account = accountService.getAccountWithDefaults(account.getUuid());
    assertThat(account).isNotNull();
    assertThat(account.getDefaults()).isNotEmpty().containsKeys("NAME", "NAME2");
    assertThat(account.getDefaults()).isNotEmpty().containsValues("", "VALUE");
    verify(settingsService).listAccountDefaults(account.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccountBreadcrumb() {
    // setup
    setupCvServicesTests(accountId, serviceId + "-test", envId, appId, cvConfigId + "-test", workflowId, user);
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("0").build();

    // test behavior
    List<Service> cvConfigs = accountService.getServicesBreadCrumb(accountId, user);

    // verify results
    assertThat(cvConfigs.size() == 2).isTrue();
    assertThat(serviceId.equals(cvConfigs.get(0).getUuid()) || serviceId.equals(cvConfigs.get(1).getUuid())).isTrue();
    assertEquals("Service name should be same", "serviceTest", cvConfigs.get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccount() {
    // setup
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("0").build();

    // test behavior
    PageResponse<CVEnabledService> cvConfigs = accountService.getServices(accountId, user, request, null);

    // verify results
    assertThat(cvConfigs.getResponse().size() > 0).isTrue();
    assertEquals("Service id should be same", serviceId, cvConfigs.getResponse().get(0).getService().getUuid());
    assertEquals("Offset correct in the page response", cvConfigs.getOffset(), "1");
    assertEquals("Service name should be same", "serviceTest", cvConfigs.getResponse().get(0).getService().getName());
    assertEquals(
        "CVConfigType name should be same", "NewRelic", cvConfigs.getResponse().get(0).getCvConfig().get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccountDisabledCVConfig() {
    // setup
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    // Save one with isEnabled set to false
    CVConfiguration config = NewRelicCVServiceConfiguration.builder().build();
    config.setAccountId(accountId);
    config.setServiceId(serviceId);
    config.setEnvId(envId);
    config.setAppId(appId);
    config.setEnabled24x7(false);
    config.setUuid(cvConfigId + "-disabled");
    config.setName("NewRelic-disabled");
    config.setStateType(StateType.NEW_RELIC);
    wingsPersistence.save(config);

    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("0").build();

    // test behavior
    PageResponse<CVEnabledService> cvConfigs = accountService.getServices(accountId, user, request, null);

    // verify results
    assertThat(cvConfigs.getResponse().size() == 1).isTrue();
    assertThat(cvConfigs.getResponse().get(0).getCvConfig().size() == 1).isTrue();
    assertEquals("Service id should be same", serviceId, cvConfigs.getResponse().get(0).getService().getUuid());
    assertEquals("Offset correct in the page response", cvConfigs.getOffset(), "1");
    assertEquals("Service name should be same", "serviceTest", cvConfigs.getResponse().get(0).getService().getName());
    assertEquals(
        "CVConfigType name should be same", "NewRelic", cvConfigs.getResponse().get(0).getCvConfig().get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccountSpecificService() {
    // setup
    setupCvServicesTests(accountId, serviceId + "-test", envId, appId, cvConfigId + "-test", workflowId, user);
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("0").build();

    // test behavior
    PageResponse<CVEnabledService> cvConfigs = accountService.getServices(accountId, user, request, serviceId);

    // verify results
    assertThat(cvConfigs.getResponse().size() == 1).isTrue();
    assertEquals("Service id should be same", serviceId, cvConfigs.getResponse().get(0).getService().getUuid());
    assertEquals("Offset correct in the page response", cvConfigs.getOffset(), "1");
    assertEquals("Service name should be same", "serviceTest", cvConfigs.getResponse().get(0).getService().getName());
    assertEquals(
        "CVConfigType name should be same", "NewRelic", cvConfigs.getResponse().get(0).getCvConfig().get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccountLastOffset() {
    // setup
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("1").build();

    // test behavior
    PageResponse<CVEnabledService> services = accountService.getServices(accountId, user, request, null);

    // verify results
    assertThat(services.getResponse().size() == 0).isTrue();
  }

  private void setupCvServicesTests(
      String accountId, String serviceId, String envId, String appId, String cvConfigId, String workflowId, User user) {
    CVConfiguration config = NewRelicCVServiceConfiguration.builder().build();
    config.setAccountId(accountId);
    config.setServiceId(serviceId);
    config.setEnvId(envId);
    config.setAppId(appId);
    config.setEnabled24x7(true);
    config.setUuid(cvConfigId);
    config.setName("NewRelic");
    config.setStateType(StateType.NEW_RELIC);

    wingsPersistence.save(Environment.Builder.anEnvironment().appId(appId).uuid(envId).build());

    wingsPersistence.saveAndGet(
        Service.class, Service.builder().name("serviceTest").appId(appId).uuid(serviceId).build());
    wingsPersistence.save(Application.Builder.anApplication().uuid(appId).name("appName").build());
    wingsPersistence.save(config);
    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, buildAppPermissionSummary(serviceId, workflowId, envId)); }
    });

    when(authService.getUserPermissionInfo(accountId, user, false)).thenReturn(mockUserPermissionInfo);
  }

  private AppPermissionSummary buildAppPermissionSummary(String serviceId, String workflowId, String envId) {
    Map<Action, Set<String>> servicePermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(serviceId, serviceId + "-test")); }
    };
    Map<Action, Set<EnvInfo>> envPermissions = new HashMap<Action, Set<EnvInfo>>() {
      {
        put(Action.READ, Sets.newHashSet(EnvInfo.builder().envId(envId).envType(EnvironmentType.PROD.name()).build()));
      }
    };
    Map<Action, Set<String>> pipelinePermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet()); }
    };
    Map<Action, Set<String>> workflowPermissions = new HashMap<Action, Set<String>>() {
      { put(Action.READ, Sets.newHashSet(workflowId)); }
    };

    return AppPermissionSummary.builder()
        .servicePermissions(servicePermissions)
        .envPermissions(envPermissions)
        .workflowPermissions(workflowPermissions)
        .pipelinePermissions(pipelinePermissions)
        .build();
  }

  /**
   * Tests if function generates unique unique account names after checking for duplicates in db
   */
  @Test
  @Category(UnitTests.class)
  public void testSuggestedAccountName() {
    // Add account
    wingsPersistence.save(anAccount()
                              .withUuid(UUID.randomUUID().toString())
                              .withCompanyName(HARNESS_NAME)
                              .withAccountName(HARNESS_NAME)
                              .build());

    // Check unique suggested account name
    String suggestion1 = accountService.suggestAccountName(HARNESS_NAME);
    assertThat(suggestion1).isNotEqualTo(HARNESS_NAME);

    // Add suggested acccount name
    wingsPersistence.save(anAccount()
                              .withUuid(UUID.randomUUID().toString())
                              .withCompanyName(HARNESS_NAME)
                              .withAccountName(suggestion1)
                              .build());

    // Check for unique suggestions
    String suggestion2 = accountService.suggestAccountName(HARNESS_NAME);
    assertThat(suggestion2).isNotEqualTo(HARNESS_NAME);
    assertThat(suggestion2).isNotEqualTo(suggestion1);
  }

  @Test
  @Category(UnitTests.class)
  public void test_updateWhitelistedDomains_shouldTrimStringsAndIgnoreWhiteSpace() {
    Account account = accountService.save(anAccount()
                                              .withCompanyName("Company Name 1")
                                              .withAccountName("Account Name 1")
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(getLicenseInfo())
                                              .withWhitelistedDomains(new HashSet<>())
                                              .build());
    accountService.updateWhitelistedDomains(
        account.getUuid(), Sets.newHashSet(" harness.io", "harness.io ", " harness.io \t\t\t \t \t"));
    account = accountService.get(account.getUuid());
    assertEquals(Sets.newHashSet("harness.io"), account.getWhitelistedDomains());
  }

  @Test
  @Category(UnitTests.class)
  public void test_updateAccountName() {
    String companyName = "CompanyName 1";
    Account account = accountService.save(anAccount()
                                              .withCompanyName(companyName)
                                              .withAccountName("Account Name 1")
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(getLicenseInfo())
                                              .withWhitelistedDomains(new HashSet<>())
                                              .build());
    String newAccountName = "New Account Name";
    accountService.updateAccountName(account.getUuid(), newAccountName, null);
    account = accountService.get(account.getUuid());
    assertEquals(newAccountName, account.getAccountName());
    assertEquals(companyName, account.getCompanyName());
  }
}
