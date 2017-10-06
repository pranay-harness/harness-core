package software.wings.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.exception.WingsException;
import software.wings.service.impl.elk.ElkDelegateServiceImpl;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.sm.StateType;

import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/2/17.
 */
public class ElkConfigurationValidationTest extends WingsBaseTest {
  private String accountId;

  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private AnalysisService analysisService;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testElkConfigNoPassword() throws Exception {
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new ElkDelegateServiceImpl());
    Whitebox.setInternalState(analysisService, "delegateProxyFactory", delegateProxyFactory);
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setUrl("some url");
    elkConfig.setUsername("someuser");

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK);
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      Assert.assertEquals("User name is given but password is empty", e.getParams().get("reason"));
    }
  }

  @Test
  public void testElkConfigNoUserName() throws Exception {
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new ElkDelegateServiceImpl());
    Whitebox.setInternalState(analysisService, "delegateProxyFactory", delegateProxyFactory);
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setUrl("some url");
    elkConfig.setPassword("somepwd".toCharArray());

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK);
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      Assert.assertEquals("User name is empty but password is given", e.getParams().get("reason"));
    }
  }

  @Test
  public void testInvalidUrl() throws Exception {
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new ElkDelegateServiceImpl());
    Whitebox.setInternalState(analysisService, "delegateProxyFactory", delegateProxyFactory);
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setUrl("some url");

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK);
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      Assert.assertEquals("Illegal URL: some url/", e.getParams().get("reason"));
    }
  }

  @Test
  public void testValidConfig() throws Exception {
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(new ElkDelegateServiceImpl());
    Whitebox.setInternalState(analysisService, "delegateProxyFactory", delegateProxyFactory);
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setUrl("https://ec2-34-207-78-53.compute-1.amazonaws.com:9200");
    elkConfig.setUsername("elastic");
    elkConfig.setPassword("W!ngs@elastic".toCharArray());

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    analysisService.validateConfig(settingAttribute, StateType.ELK);
  }
}
