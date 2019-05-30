package software.wings.service;

import static io.harness.rule.OwnerRule.RAGHU;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.OwnerRule.Owner;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.WingsBaseTest;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.StateType;

import java.util.Collections;
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
  @Inject private ScmSecret scmSecret;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = UUID.randomUUID().toString();
    MockitoAnnotations.initMocks(this);
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(elkDelegateService);
    FieldUtils.writeField(analysisService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(elkDelegateService, "encryptionService", encryptionService, true);
  }

  @Test
  @Owner(emails = RAGHU, intermittent = true)
  @Category(UnitTests.class)
  public void testElkConfigNoPassword() {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("some url");
    elkConfig.setUsername("someuser");

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK, Collections.emptyList());
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      assertEquals("WingsException: User name is given but password is empty", e.getParams().get("reason"));
    }
  }

  @Test
  @Owner(emails = RAGHU, intermittent = true)
  @Category(UnitTests.class)
  public void testElkConfigNoUserName() {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("some url");
    elkConfig.setPassword("somepwd".toCharArray());

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK, Collections.emptyList());
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      assertEquals("WingsException: User name is empty but password is given", e.getParams().get("reason"));
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testInvalidUrl() {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("some url");

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    try {
      analysisService.validateConfig(settingAttribute, StateType.ELK, Collections.emptyList());
      Assert.fail("validated invalid config");
    } catch (WingsException e) {
      assertEquals("IllegalArgumentException: Illegal URL: some url/", e.getParams().get("reason"));
    }
  }

  @Test
  @Owner(emails = RAGHU)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void testValidConfig() {
    final ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(accountId);
    elkConfig.setElkUrl("https://ec2-34-207-78-53.compute-1.amazonaws.com:9200");
    elkConfig.setUsername("elastic");
    elkConfig.setPassword(scmSecret.decryptToCharArray(new SecretName("elk_config_elastic_user_password")));

    final SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute().withAccountId(accountId).withValue(elkConfig).build();
    analysisService.validateConfig(settingAttribute, StateType.ELK, Collections.emptyList());
  }
}
