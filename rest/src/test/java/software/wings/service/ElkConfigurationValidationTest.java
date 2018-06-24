package software.wings.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;

import java.util.UUID;

/**
 * Created by rsingh on 10/2/17.
 */
public class ElkConfigurationValidationTest extends WingsBaseTest {
  private String accountId;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private AnalysisService analysisService;
  @Inject private ElkDelegateService elkDelegateService;
  @Inject private EncryptionService encryptionService;
  @Inject private WingsPersistence wingsPersistence;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(elkDelegateService);
    setInternalState(analysisService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(elkDelegateService, "encryptionService", encryptionService);
  }

  @Test
  public void testElkConfigNoPassword() throws Exception {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("some url");
    elkConfig.setUsername("someuser");

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK);
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      assertEquals("User name is given but password is empty", e.getParams().get("reason"));
    }
  }

  @Test
  public void testElkConfigNoUserName() throws Exception {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("some url");
    elkConfig.setPassword("somepwd".toCharArray());

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK);
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      assertEquals("User name is empty but password is given", e.getParams().get("reason"));
    }
  }

  @Test
  public void testInvalidUrl() throws Exception {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("some url");

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK);
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      assertEquals("IllegalArgumentException: Illegal URL: some url/", e.getParams().get("reason"));
    }
  }

  @Test
  @Ignore
  public void testValidConfig() throws Exception {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("https://ec2-34-207-78-53.compute-1.amazonaws.com:9200");
    elkConfig.setUsername("elastic");
    elkConfig.setPassword("W!ngs@elastic".toCharArray());

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    analysisService.validateConfig(settingAttribute, StateType.ELK);
  }
}
