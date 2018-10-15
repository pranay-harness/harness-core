package software.wings.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.rule.RepeatRule.Repeat;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.User;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.generator.SecretGenerator;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by rsingh on 10/10/17.
 */
public class AppdynamicsTest extends WingsBaseTest {
  @Inject private AppdynamicsService appdynamicsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;
  @Inject private SecretGenerator secretGenerator;
  @Inject private ScmSecret scmSecret;
  @Mock private DelegateProxyFactory appdDelegateProxyFactory;
  @Mock private DelegateProxyFactory kmsDelegateProxyFactory;
  private SettingAttribute settingAttribute;
  private String accountId;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().withEmail(userEmail).withName(userName).build();

  @Before
  public void setup() {
    initMocks(this);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);
    setInternalState(appdynamicsDelegateService, "encryptionService", encryptionService);
    when(appdDelegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(appdynamicsDelegateService);
    setInternalState(appdynamicsService, "delegateProxyFactory", appdDelegateProxyFactory);

    accountId = UUID.randomUUID().toString();

    AppDynamicsConfig appDynamicsConfig =
        AppDynamicsConfig.builder()
            .accountId(accountId)
            .controllerUrl("https://harness-test.saas.appdynamics.com/controller")
            .accountname("harness-test")
            .username("raghu@harness.io")
            .password(scmSecret.decryptToCharArray(new SecretName("appd_config_password")))
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
  @Ignore
  @Repeat(times = 5, successes = 1)
  public void validateConfig() {
    ((AppDynamicsConfig) settingAttribute.getValue())
        .setPassword(scmSecret.decryptToCharArray(new SecretName("appd_config_password")));
    appdynamicsService.validateConfig(settingAttribute);
  }

  @Test
  @Ignore
  @Repeat(times = 5, successes = 1)
  public void getAllApplications() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertFalse(applications.isEmpty());
  }

  @Test
  @Ignore
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
  @Ignore
  @Repeat(times = 5, successes = 1)
  public void getDependentTiers() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertFalse(applications.isEmpty());
    for (NewRelicApplication appDApp : applications) {
      Set<AppdynamicsTier> tiers = appdynamicsService.getTiers(settingAttribute.getUuid(), appDApp.getId());
      assertFalse(tiers.isEmpty());

      for (AppdynamicsTier tier : tiers) {
        if (tier.getName().equals("docker-tier")) {
          Set<AppdynamicsTier> dependentTiers =
              appdynamicsService.getDependentTiers(settingAttribute.getUuid(), appDApp.getId(), tier);
          assertTrue(isEmpty(dependentTiers));
        }
      }
    }
  }
}
