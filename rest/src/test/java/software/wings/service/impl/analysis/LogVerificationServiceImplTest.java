package software.wings.service.impl.analysis;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.annotation.Encryptable;
import software.wings.beans.BugsnagConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.impl.bugsnag.BugsnagDelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.analysis.LogVerificationServiceImpl;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.StateType;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

public class LogVerificationServiceImplTest {
  @Mock private SettingsService mockSettingsService;
  @Mock private DelegateProxyFactory mockDelegateProxyFactory;
  @Mock private BugsnagDelegateService mockBugsnagDelegateService;
  @Mock private SecretManager mockSecretManager;
  @InjectMocks LogVerificationServiceImpl service;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetBugsnagOrgs() {
    BugsnagConfig config = new BugsnagConfig();
    config.setUrl("testBugsnagURL");
    config.setAccountId("myaccountId");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    BugsnagApplication application = BugsnagApplication.builder().id("orgId").name("orgName").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockBugsnagDelegateService);
    when(mockSecretManager.getEncryptionDetails(any(Encryptable.class), anyString(), anyString())).thenReturn(null);
    when(mockBugsnagDelegateService.getOrganizations(config, null, null))
        .thenReturn(new TreeSet<>(Arrays.asList(application)));

    // execute
    Set<BugsnagApplication> appList = service.getOrgProjectListBugsnag("setting", "", StateType.BUG_SNAG, false);

    assertEquals(1, appList.size());
  }

  @Test
  public void testGetBugsnagApplications() {
    BugsnagConfig config = new BugsnagConfig();
    config.setUrl("testBugsnagURL");
    config.setAccountId("myaccountId");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    BugsnagApplication application = BugsnagApplication.builder().id("orgId").name("orgName").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockBugsnagDelegateService);
    when(mockSecretManager.getEncryptionDetails(any(Encryptable.class), anyString(), anyString())).thenReturn(null);
    when(mockBugsnagDelegateService.getProjects(config, "orgId", null, null))
        .thenReturn(new TreeSet<>(Arrays.asList(application)));

    // execute
    Set<BugsnagApplication> appList = service.getOrgProjectListBugsnag("setting", "orgId", StateType.BUG_SNAG, true);

    assertEquals(1, appList.size());
  }

  @Test(expected = WingsException.class)
  public void testGetBugsnagApplicationsBadState() {
    BugsnagConfig config = new BugsnagConfig();
    config.setUrl("testBugsnagURL");
    config.setAccountId("myaccountId");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    BugsnagApplication application = BugsnagApplication.builder().id("orgId").name("orgName").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockBugsnagDelegateService);
    when(mockSecretManager.getEncryptionDetails(any(Encryptable.class), anyString(), anyString())).thenReturn(null);
    when(mockBugsnagDelegateService.getOrganizations(config, null, null))
        .thenReturn(new TreeSet<>(Arrays.asList(application)));

    // execute
    Set<BugsnagApplication> appList = service.getOrgProjectListBugsnag("setting", "", StateType.NEW_RELIC, false);

    assertEquals(1, appList.size());
  }
}
