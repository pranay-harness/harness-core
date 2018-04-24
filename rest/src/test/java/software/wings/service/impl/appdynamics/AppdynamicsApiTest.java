package software.wings.service.impl.appdynamics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.resources.AppdynamicsResource;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Created by rsingh on 4/24/18.
 */
public class AppdynamicsApiTest extends WingsBaseTest {
  @Inject private AppdynamicsResource appdynamicsResource;
  @Inject private AppdynamicsService appdynamicsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private AppdynamicsRestClient appdynamicsRestClient;

  private AppdynamicsDelegateServiceImpl delegateService;
  private String accountId;

  @Before
  public void setup() {
    delegateService = spy(new AppdynamicsDelegateServiceImpl());
    doReturn(appdynamicsRestClient).when(delegateService).getAppdynamicsRestClient(any(AppDynamicsConfig.class));
    when(delegateProxyFactory.get(eq(AppdynamicsDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(delegateService);

    setInternalState(appdynamicsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(appdynamicsResource, "appdynamicsService", appdynamicsService);
    setInternalState(delegateService, "encryptionService", encryptionService);
    accountId = UUID.randomUUID().toString();
  }

  @Test
  public void testGetApplications() throws IOException {
    Call<List<NewRelicApplication>> restCall = Mockito.mock(Call.class);
    List<NewRelicApplication> applications = Lists.newArrayList(
        NewRelicApplication.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build(),
        NewRelicApplication.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build());
    when(restCall.execute()).thenReturn(Response.success(applications));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();

    RestResponse<List<NewRelicApplication>> allApplications =
        appdynamicsResource.getAllApplications(accountId, savedAttributeId);
    assertTrue(allApplications.getResponseMessages().isEmpty());
    assertEquals(applications, allApplications.getResource());
  }

  @Test
  public void testGetTiers() throws IOException {
    Call<List<AppdynamicsTier>> restCall = Mockito.mock(Call.class);
    List<AppdynamicsTier> tiers = Lists.newArrayList(
        AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build(),
        AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build());
    when(restCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.listTiers(anyString(), anyLong())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();

    RestResponse<List<AppdynamicsTier>> allTiers =
        appdynamicsResource.getAllTiers(accountId, savedAttributeId, new Random().nextLong());
    assertTrue(allTiers.getResponseMessages().isEmpty());
    assertEquals(tiers, allTiers.getResource());
  }

  @Test
  public void testGetBTs() throws IOException {
    Call<List<AppdynamicsTier>> tierRestCall = Mockito.mock(Call.class);
    AppdynamicsTier tier =
        AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build();
    List<AppdynamicsTier> tiers = Lists.newArrayList(tier);
    when(tierRestCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetric>> btsCall = Mockito.mock(Call.class);
    List<AppdynamicsMetric> bts = Lists.newArrayList(
        AppdynamicsMetric.builder().name(UUID.randomUUID().toString()).type(AppdynamicsMetricType.leaf).build(),
        AppdynamicsMetric.builder().name(UUID.randomUUID().toString()).type(AppdynamicsMetricType.leaf).build());
    when(btsCall.execute()).thenReturn(Response.success(bts));
    when(appdynamicsRestClient.listMetrices(anyString(), anyLong(), anyString())).thenReturn(btsCall);

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(UUID.randomUUID().toString().toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();
    List<AppdynamicsMetric> tierBTMetrics = delegateService.getTierBTMetrics(
        appDynamicsConfig, new Random().nextLong(), new Random().nextLong(), Collections.emptyList());
    assertEquals(2, tierBTMetrics.size());
    assertEquals(bts, tierBTMetrics);
  }

  @Test
  public void testGetBTData() throws IOException {
    Call<List<AppdynamicsTier>> tierRestCall = Mockito.mock(Call.class);
    AppdynamicsTier tier =
        AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build();
    List<AppdynamicsTier> tiers = Lists.newArrayList(tier);
    when(tierRestCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetricData>> btDataCall = Mockito.mock(Call.class);
    List<AppdynamicsMetricData> btData =
        Lists.newArrayList(AppdynamicsMetricData.builder()
                               .metricId(new Random().nextLong())
                               .frequency(UUID.randomUUID().toString())
                               .metricName(UUID.randomUUID().toString())
                               .metricPath(UUID.randomUUID().toString())
                               .metricValues(Lists
                                                 .newArrayList(AppdynamicsMetricDataValue.builder()
                                                                   .count(new Random().nextLong())
                                                                   .current(new Random().nextLong())
                                                                   .max(new Random().nextLong())
                                                                   .min(new Random().nextLong())
                                                                   .occurrences(new Random().nextInt())
                                                                   .standardDeviation(new Random().nextDouble())
                                                                   .sum(new Random().nextLong())
                                                                   .startTimeInMillis(new Random().nextLong())
                                                                   .useRange(new Random().nextBoolean())
                                                                   .value(new Random().nextDouble())
                                                                   .build(),
                                                     AppdynamicsMetricDataValue.builder()
                                                         .count(new Random().nextLong())
                                                         .current(new Random().nextLong())
                                                         .max(new Random().nextLong())
                                                         .min(new Random().nextLong())
                                                         .occurrences(new Random().nextInt())
                                                         .standardDeviation(new Random().nextDouble())
                                                         .sum(new Random().nextLong())
                                                         .startTimeInMillis(new Random().nextLong())
                                                         .useRange(new Random().nextBoolean())
                                                         .value(new Random().nextDouble())
                                                         .build())
                                                 .toArray(new AppdynamicsMetricDataValue[2]))
                               .build(),
            AppdynamicsMetricData.builder()
                .metricId(new Random().nextLong())
                .frequency(UUID.randomUUID().toString())
                .metricName(UUID.randomUUID().toString())
                .metricPath(UUID.randomUUID().toString())
                .metricValues(Lists
                                  .newArrayList(AppdynamicsMetricDataValue.builder()
                                                    .count(new Random().nextLong())
                                                    .current(new Random().nextLong())
                                                    .max(new Random().nextLong())
                                                    .min(new Random().nextLong())
                                                    .occurrences(new Random().nextInt())
                                                    .standardDeviation(new Random().nextDouble())
                                                    .sum(new Random().nextLong())
                                                    .startTimeInMillis(new Random().nextLong())
                                                    .useRange(new Random().nextBoolean())
                                                    .value(new Random().nextDouble())
                                                    .build(),
                                      AppdynamicsMetricDataValue.builder()
                                          .count(new Random().nextLong())
                                          .current(new Random().nextLong())
                                          .max(new Random().nextLong())
                                          .min(new Random().nextLong())
                                          .occurrences(new Random().nextInt())
                                          .standardDeviation(new Random().nextDouble())
                                          .sum(new Random().nextLong())
                                          .startTimeInMillis(new Random().nextLong())
                                          .useRange(new Random().nextBoolean())
                                          .value(new Random().nextDouble())
                                          .build())
                                  .toArray(new AppdynamicsMetricDataValue[2]))
                .build());
    when(btDataCall.execute()).thenReturn(Response.success(btData));
    when(appdynamicsRestClient.getMetricData(anyString(), anyLong(), anyString(), anyInt())).thenReturn(btDataCall);

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(UUID.randomUUID().toString().toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();
    List<AppdynamicsMetricData> tierBTMetricData = delegateService.getTierBTMetricData(appDynamicsConfig,
        new Random().nextLong(), new Random().nextLong(), UUID.randomUUID().toString(), UUID.randomUUID().toString(),
        new Random().nextInt(), Collections.emptyList());
    assertEquals(2, tierBTMetricData.size());
    assertEquals(btData, tierBTMetricData);
  }

  private String saveAppdynamicsConfig() {
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl(UUID.randomUUID().toString())
                                                    .username(UUID.randomUUID().toString())
                                                    .password(UUID.randomUUID().toString().toCharArray())
                                                    .accountname(UUID.randomUUID().toString())
                                                    .build();

    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(appDynamicsConfig.getAccountId())
                                            .withValue(appDynamicsConfig)
                                            .withAppId(UUID.randomUUID().toString())
                                            .withCategory(Category.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    return wingsPersistence.save(settingAttribute);
  }
}
