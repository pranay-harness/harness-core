package software.wings.service;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.rule.RepeatRule.Repeat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.KmsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.User;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by rsingh on 10/10/17.
 */
@Ignore
@RunWith(Parameterized.class)
public class AppdynamicsTest extends WingsBaseTest {
  @Inject private KmsService kmsService;
  @Inject private SecretManager secretManager;
  @Inject private SecretManagementDelegateServiceImpl kmsDelegateService;
  @Inject private AppdynamicsService appdynamicsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;
  @Mock private DelegateProxyFactory appdDelegateProxyFactory;
  @Mock private DelegateProxyFactory kmsDelegateProxyFactory;
  private SettingAttribute settingAttribute;
  private String accountId;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();

  @Parameter public boolean isKmsEnabled;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{true}, {false}});
  }

  @Before
  public void setup() {
    initMocks(this);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);
    setInternalState(appdynamicsDelegateService, "encryptionService", encryptionService);
    when(appdDelegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(appdynamicsDelegateService);
    when(kmsDelegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(kmsDelegateService);
    setInternalState(kmsService, "delegateProxyFactory", kmsDelegateProxyFactory);
    setInternalState(appdynamicsService, "delegateProxyFactory", appdDelegateProxyFactory);
    setInternalState(wingsPersistence, "secretManager", secretManager);
    setInternalState(secretManager, "kmsService", kmsService);

    accountId = UUID.randomUUID().toString();

    if (isKmsEnabled) {
      final KmsConfig kmsConfig = getKmsConfig();
      kmsService.saveKmsConfig(accountId, kmsConfig);
    }

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl("https://harness-test.saas.appdynamics.com/controller")
                                              .username("raghu@harness.io")
                                              .accountname("harness-test")
                                              .password("(idlk2e9idcs@ej".toCharArray())
                                              .build();
    settingAttribute = aSettingAttribute()
                           .withName("AppD")
                           .withCategory(Category.CONNECTOR)
                           .withAccountId(accountId)
                           .withValue(appDynamicsConfig)
                           .build();
    wingsPersistence.save(settingAttribute);
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void validateConfig() {
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword("(idlk2e9idcs@ej".toCharArray());
    appdynamicsService.validateConfig(settingAttribute);
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getAllApplications() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertFalse(applications.isEmpty());
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void getTiers() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertFalse(applications.isEmpty());
    for (NewRelicApplication appDApp : applications) {
      Set<AppdynamicsTier> tiers = appdynamicsService.getTiers(settingAttribute.getUuid(), appDApp.getId());
      assertFalse(tiers.isEmpty());
    }
  }

  @Test
  public void xyzn() throws Exception {
    Set<AppdynamicsTier> dependentTiers = appdynamicsService.getDependentTiers(
        settingAttribute.getUuid(), 6719, AppdynamicsTier.builder().id(135482).name("tier1").build());
    System.out.println(dependentTiers);
  }

  private KmsConfig getKmsConfig() {
    final KmsConfig kmsConfig = new KmsConfig();
    kmsConfig.setName("myKms");
    kmsConfig.setDefault(true);
    kmsConfig.setKmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d");
    kmsConfig.setAccessKey("AKIAJLEKM45P4PO5QUFQ");
    kmsConfig.setSecretKey("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");
    return kmsConfig;
  }
}
