package software.wings.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.rule.OwnerRule.PRANJAL;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import io.harness.rule.Repeat;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.User;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Collections;
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
  @Inject private ScmSecret scmSecret;
  @Mock private DelegateProxyFactory appdDelegateProxyFactory;
  @Mock private DelegateProxyFactory kmsDelegateProxyFactory;
  private SettingAttribute settingAttribute;
  private String accountId;
  private final String userEmail = "rsingh@harness.io";
  private final String userName = "raghu";
  private final User user = User.Builder.anUser().email(userEmail).name(userName).build();

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    wingsPersistence.save(user);
    UserThreadLocal.set(user);
    FieldUtils.writeField(appdynamicsDelegateService, "encryptionService", encryptionService, true);
    when(appdDelegateProxyFactory.get(anyObject(), any(SyncTaskContext.class))).thenReturn(appdynamicsDelegateService);
    FieldUtils.writeField(appdynamicsService, "delegateProxyFactory", appdDelegateProxyFactory, true);

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
                           .withCategory(SettingCategory.CONNECTOR)
                           .withAccountId(accountId)
                           .withValue(appDynamicsConfig)
                           .build();
    wingsPersistence.save(settingAttribute);
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void validateConfig() {
    ((AppDynamicsConfig) settingAttribute.getValue())
        .setPassword(scmSecret.decryptToCharArray(new SecretName("appd_config_password")));
    appdynamicsService.validateConfig(settingAttribute, Collections.emptyList());
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(UnitTests.class)
  public void validateConfigInvalidURL() {
    ((AppDynamicsConfig) settingAttribute.getValue())
        .setPassword(scmSecret.decryptToCharArray(new SecretName("appd_config_password")));
    ((AppDynamicsConfig) settingAttribute.getValue())
        .setControllerUrl("https://appd-bi-alpha-test.company.intranet/controller");

    thrown.expect(WingsException.class);
    appdynamicsService.validateConfig(settingAttribute, Collections.emptyList());
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void getAllApplications() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertThat(applications.isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void getTiers() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertThat(applications.isEmpty()).isFalse();
    Set<AppdynamicsTier> tiers =
        appdynamicsService.getTiers(settingAttribute.getUuid(), applications.iterator().next().getId());
    assertThat(tiers.isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = RAGHU)
  @Repeat(times = 5, successes = 1)
  @Category(UnitTests.class)
  public void getDependentTiers() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertThat(applications.isEmpty()).isFalse();
    NewRelicApplication app = applications.iterator().next();
    Set<AppdynamicsTier> tiers = appdynamicsService.getTiers(settingAttribute.getUuid(), app.getId());
    assertThat(tiers.isEmpty()).isFalse();

    AppdynamicsTier tier = tiers.iterator().next();
    if (tier.getName().equals("docker-tier")) {
      Set<AppdynamicsTier> dependentTiers =
          appdynamicsService.getDependentTiers(settingAttribute.getUuid(), app.getId(), tier);
      assertThat(isEmpty(dependentTiers)).isTrue();
    }
  }
}
