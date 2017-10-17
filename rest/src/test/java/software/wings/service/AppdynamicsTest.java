package software.wings.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.rules.RealMongo;
import software.wings.rules.RepeatRule.Repeat;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

/**
 * Created by rsingh on 10/10/17.
 */
public class AppdynamicsTest extends WingsBaseTest {
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;
  @Inject private AppdynamicsService appdynamicsService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private WingsPersistence wingsPersistence;
  private SettingAttribute settingAttribute;
  private String accountId;

  @Before
  public void setup() {
    accountId = UUID.randomUUID().toString();
    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl("https://wingsnfr.saas.appdynamics.com/controller")
                                              .accountname("wingsnfr")
                                              .username("wingsnfr")
                                              .password("cbm411sjesma".toCharArray())
                                              .build();
    settingAttribute = aSettingAttribute()
                           .withName("AppD")
                           .withCategory(Category.CONNECTOR)
                           .withAccountId(accountId)
                           .withValue(appDynamicsConfig)
                           .build();
    wingsPersistence.save(settingAttribute);
    MockitoAnnotations.initMocks(this);
    Mockito.when(delegateProxyFactory.get(Mockito.anyObject(), Mockito.any(SyncTaskContext.class)))
        .thenReturn(appdynamicsDelegateService);
    Whitebox.setInternalState(appdynamicsService, "delegateProxyFactory", delegateProxyFactory);
  }

  @Test
  @RealMongo
  @Repeat(times = 5, successes = 1)
  public void validateConfig() {
    appdynamicsService.validateConfig(settingAttribute);
  }

  @Test
  @RealMongo
  @Repeat(times = 5, successes = 1)
  public void getAllApplications() throws IOException {
    List<NewRelicApplication> applications = appdynamicsService.getApplications(settingAttribute.getUuid());
    assertFalse(applications.isEmpty());
  }

  @Test
  @RealMongo
  @Repeat(times = 5, successes = 1)
  public void getTiers() throws IOException {
    NewRelicApplication application = getDemoApp();
    List<AppdynamicsTier> tiers = appdynamicsService.getTiers(settingAttribute.getUuid(), application.getId());
    assertFalse(tiers.isEmpty());
  }

  @Test
  @RealMongo
  @Repeat(times = 5, successes = 1)
  public void getNodes() throws IOException {
    NewRelicApplication application = getDemoApp();
    List<AppdynamicsTier> tiers = appdynamicsService.getTiers(settingAttribute.getUuid(), application.getId());
    assertFalse(tiers.isEmpty());
    for (AppdynamicsTier tier : tiers) {
      List<AppdynamicsNode> nodes =
          appdynamicsService.getNodes(settingAttribute.getUuid(), application.getId(), tier.getId());
      assertFalse(nodes.isEmpty());
    }
  }

  @Test
  @RealMongo
  @Repeat(times = 5, successes = 1)
  public void getBusinessTransactions() throws IOException {
    NewRelicApplication application = getDemoApp();
    List<AppdynamicsBusinessTransaction> businessTransactions =
        appdynamicsService.getBusinessTransactions(settingAttribute.getUuid(), application.getId());
    assertFalse(businessTransactions.isEmpty());
  }

  @Test
  @RealMongo
  @Repeat(times = 5, successes = 1)
  public void getTierBTMetrics() throws IOException {
    NewRelicApplication application = getDemoApp();
    List<AppdynamicsTier> tiers = appdynamicsService.getTiers(settingAttribute.getUuid(), application.getId());
    assertFalse(tiers.isEmpty());
    for (AppdynamicsTier tier : tiers) {
      List<AppdynamicsMetric> btMetrics =
          appdynamicsService.getTierBTMetrics(settingAttribute.getUuid(), application.getId(), tier.getId());
      assertFalse(btMetrics.isEmpty());
    }
  }

  @Test
  @RealMongo
  @Repeat(times = 5, successes = 1)
  public void getTierBTMetricData() throws IOException {
    NewRelicApplication application = getDemoApp();
    List<AppdynamicsTier> tiers = appdynamicsService.getTiers(settingAttribute.getUuid(), application.getId());
    assertFalse(tiers.isEmpty());
    for (AppdynamicsTier tier : tiers) {
      List<AppdynamicsMetric> btMetrics =
          appdynamicsService.getTierBTMetrics(settingAttribute.getUuid(), application.getId(), tier.getId());
      assertFalse(btMetrics.isEmpty());
      for (AppdynamicsMetric btMetric : btMetrics) {
        List<AppdynamicsMetricData> tierBTMetricData = appdynamicsService.getTierBTMetricData(
            settingAttribute.getUuid(), application.getId(), tier.getId(), btMetric.getName(), 5);
        assertNotNull(tierBTMetricData);
      }
    }
  }

  private NewRelicApplication getDemoApp() throws IOException {
    List<NewRelicApplication> allApplications = appdynamicsService.getApplications(settingAttribute.getUuid());
    for (NewRelicApplication application : allApplications) {
      if (application.getName().equals("appd-integration")) {
        return application;
      }
    }

    throw new IllegalStateException("Could not find application appd-integration");
  }
}
