package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
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
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.service.impl.analysis.CVEnabledService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
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
  @Mock private SettingsService settingsService;
  @Mock private TemplateGalleryService templateGalleryService;
  @Mock private UserPermissionInfo mockUserPermissionInfo;
  @Mock private AuthService authService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration configuration;

  @InjectMocks @Inject private LicenseService licenseService;
  @InjectMocks @Inject private AccountService accountService;

  @Inject private WingsPersistence wingsPersistence;

  @Rule public ExpectedException thrown = ExpectedException.none();
  private static final String HARNESS_NAME = "Harness";

  @Before
  public void setup() {
    setInternalState(licenseService, "accountService", accountService);
    setInternalState(accountService, "licenseService", licenseService);
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

  @Test
  @Category(UnitTests.class)
  public void testGetAccountType() {
    LicenseInfo licenseInfo = getLicenseInfo();
    licenseInfo.setAccountType(AccountType.FREE);

    Account account = accountService.save(anAccount()
                                              .withCompanyName(HARNESS_NAME)
                                              .withAccountName(HARNESS_NAME)
                                              .withAccountKey("ACCOUNT_KEY")
                                              .withLicenseInfo(licenseInfo)
                                              .build());

    assertThat(accountService.getAccountType(account.getUuid())).isEqualTo(Optional.of(AccountType.FREE));
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
    when(configuration.getPortal().getAllowedDomainsList()).thenReturn(new ArrayList<>());

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
    Account account = wingsPersistence.saveAndGet(
        Account.class, anAccount().withCompanyName("Wings").withAccountName("Wings").build());
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
    String serviceId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String workflowId = UUID.randomUUID().toString();
    String cvConfigId = UUID.randomUUID().toString();
    User user = new User();

    // setup
    setupCvServicesTests(accountId, serviceId + "-test", envId, appId, cvConfigId + "-test", workflowId, user);
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("0").build();

    // test behavior
    List<Service> cvConfigs = accountService.getServicesBreadCrumb(accountId, user);

    // verify results
    assertTrue("Service list should size 2", cvConfigs.size() == 2);
    assertTrue("Service id should be same",
        serviceId.equals(cvConfigs.get(0).getUuid()) || serviceId.equals(cvConfigs.get(1).getUuid()));
    assertEquals("Service name should be same", "serviceTest", cvConfigs.get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccount() {
    String serviceId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String workflowId = UUID.randomUUID().toString();
    String cvConfigId = UUID.randomUUID().toString();
    User user = new User();

    // setup
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("0").build();

    // test behavior
    PageResponse<CVEnabledService> cvConfigs = accountService.getServices(accountId, user, request, null);

    // verify results
    assertTrue("Service list should not be empty", cvConfigs.getResponse().size() > 0);
    assertEquals("Service id should be same", serviceId, cvConfigs.getResponse().get(0).getService().getUuid());
    assertEquals("Offset correct in the page response", cvConfigs.getOffset(), "1");
    assertEquals("Service name should be same", "serviceTest", cvConfigs.getResponse().get(0).getService().getName());
    assertEquals(
        "CVConfigType name should be same", "NewRelic", cvConfigs.getResponse().get(0).getCvConfig().get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccountDisabledCVConfig() {
    String serviceId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String workflowId = UUID.randomUUID().toString();
    String cvConfigId = UUID.randomUUID().toString();
    User user = new User();

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
    assertTrue("Service list should not be empty", cvConfigs.getResponse().size() == 1);
    assertTrue("Config list should be size 1", cvConfigs.getResponse().get(0).getCvConfig().size() == 1);
    assertEquals("Service id should be same", serviceId, cvConfigs.getResponse().get(0).getService().getUuid());
    assertEquals("Offset correct in the page response", cvConfigs.getOffset(), "1");
    assertEquals("Service name should be same", "serviceTest", cvConfigs.getResponse().get(0).getService().getName());
    assertEquals(
        "CVConfigType name should be same", "NewRelic", cvConfigs.getResponse().get(0).getCvConfig().get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccountSpecificService() {
    String serviceId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String workflowId = UUID.randomUUID().toString();
    String cvConfigId = UUID.randomUUID().toString();
    User user = new User();

    // setup
    setupCvServicesTests(accountId, serviceId + "-test", envId, appId, cvConfigId + "-test", workflowId, user);
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("0").build();

    // test behavior
    PageResponse<CVEnabledService> cvConfigs = accountService.getServices(accountId, user, request, serviceId);

    // verify results
    assertTrue("Service list should size 1", cvConfigs.getResponse().size() == 1);
    assertEquals("Service id should be same", serviceId, cvConfigs.getResponse().get(0).getService().getUuid());
    assertEquals("Offset correct in the page response", cvConfigs.getOffset(), "1");
    assertEquals("Service name should be same", "serviceTest", cvConfigs.getResponse().get(0).getService().getName());
    assertEquals(
        "CVConfigType name should be same", "NewRelic", cvConfigs.getResponse().get(0).getCvConfig().get(0).getName());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetServicesForAccountLastOffset() {
    String serviceId = UUID.randomUUID().toString();
    String envId = UUID.randomUUID().toString();
    String accountId = UUID.randomUUID().toString();
    String appId = UUID.randomUUID().toString();
    String workflowId = UUID.randomUUID().toString();
    String cvConfigId = UUID.randomUUID().toString();
    User user = new User();

    // setup
    setupCvServicesTests(accountId, serviceId, envId, appId, cvConfigId, workflowId, user);
    PageRequest<String> request = PageRequestBuilder.aPageRequest().withOffset("1").build();

    // test behavior
    PageResponse<CVEnabledService> services = accountService.getServices(accountId, user, request, null);

    // verify results
    assertTrue("Service list should be empty", services.getResponse().size() == 0);
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

    wingsPersistence.save(Environment.Builder.anEnvironment().withAppId(appId).withUuid(envId).build());

    wingsPersistence.saveAndGet(
        Service.class, Service.builder().name("serviceTest").appId(appId).uuid(serviceId).build());
    wingsPersistence.save(Application.Builder.anApplication().withUuid(appId).withName("appName").build());
    wingsPersistence.save(config);
    when(mockUserPermissionInfo.getAppPermissionMapInternal()).thenReturn(new HashMap<String, AppPermissionSummary>() {
      { put(appId, buildAppPermissionSummary(serviceId, workflowId, envId)); }
    });

    when(authService.getUserPermissionInfo(accountId, user)).thenReturn(mockUserPermissionInfo);
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
}
