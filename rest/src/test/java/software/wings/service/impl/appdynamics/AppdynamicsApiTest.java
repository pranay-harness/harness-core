package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.impl.ThirdPartyApiCallLog.apiCallLogWithDummyStateExecution;
import static software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl.BT_PERFORMANCE_PATH_PREFIX;
import static software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl.EXTERNAL_CALLS;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import okhttp3.internal.http.RealResponseBody;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.resources.AppdynamicsResource;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
  @Mock private DelegateLogService delegateLogService;

  private AppdynamicsDelegateServiceImpl delegateService;
  private String accountId;

  @Before
  public void setup() {
    delegateService = spy(new AppdynamicsDelegateServiceImpl());
    doReturn(appdynamicsRestClient).when(delegateService).getAppdynamicsRestClient(any(AppDynamicsConfig.class));
    when(delegateProxyFactory.get(eq(AppdynamicsDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(delegateService);
    doNothing().when(delegateLogService).save(anyString(), any(ThirdPartyApiCallLog.class));

    setInternalState(appdynamicsService, "delegateProxyFactory", delegateProxyFactory);
    setInternalState(appdynamicsResource, "appdynamicsService", appdynamicsService);
    setInternalState(delegateService, "encryptionService", encryptionService);
    setInternalState(delegateService, "delegateLogService", delegateLogService);
    accountId = UUID.randomUUID().toString();
  }

  @Test
  public void testUnreachableAppdynamicsServer() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    RuntimeException runtimeException = new RuntimeException(UUID.randomUUID().toString());
    when(restCall.execute()).thenThrow(runtimeException);
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    try {
      appdynamicsService.validateConfig(settingAttribute);
      fail("Validated invalid config");
    } catch (WingsException e) {
      assertEquals(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR, e.getCode());
      assertEquals(
          "Could not reach AppDynamics server. " + Misc.getMessage(runtimeException), e.getParams().get("reason"));
    }
  }

  @Test
  public void testInvalidCredential() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    when(restCall.execute()).thenReturn(Response.error(HttpStatus.SC_UNAUTHORIZED, new RealResponseBody(null, null)));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    try {
      appdynamicsService.validateConfig(settingAttribute);
      fail("Validated invalid config");
    } catch (WingsException e) {
      assertEquals(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR, e.getCode());
      assertEquals("Could not login to AppDynamics server with the given credentials", e.getParams().get("reason"));
    }
  }

  @Test
  public void testValidConfig() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    when(restCall.execute()).thenReturn(Response.success(Collections.emptyList()));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    assertTrue(appdynamicsService.validateConfig(settingAttribute));
  }

  @Test
  public void testGetApplications() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    List<NewRelicApplication> applications = Lists.newArrayList(
        NewRelicApplication.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build(),
        NewRelicApplication.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build());

    List<NewRelicApplication> sortedApplicationsByName =
        applications.stream()
            .sorted(Comparator.comparing(application -> application.getName()))
            .collect(Collectors.toList());

    when(restCall.execute()).thenReturn(Response.success(applications));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();

    RestResponse<List<NewRelicApplication>> allApplications =
        appdynamicsResource.getAllApplications(accountId, savedAttributeId);
    assertTrue(allApplications.getResponseMessages().isEmpty());
    assertEquals(sortedApplicationsByName, allApplications.getResource());
  }

  @Test
  public void testGetTiers() throws IOException {
    Call<Set<AppdynamicsTier>> restCall = mock(Call.class);
    Set<AppdynamicsTier> tiers =
        Sets.newHashSet(AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build(),
            AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build());
    when(restCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.listTiers(anyString(), anyLong())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();

    RestResponse<Set<AppdynamicsTier>> allTiers =
        appdynamicsResource.getAllTiers(accountId, savedAttributeId, new Random().nextLong());
    assertTrue(allTiers.getResponseMessages().isEmpty());
    assertEquals(tiers, allTiers.getResource());
  }

  @Test
  @Ignore
  public void testGetTierDependencies() throws IOException {
    int numOfTiers = 5;
    Call<Set<AppdynamicsTier>> restCall = mock(Call.class);
    Set<AppdynamicsTier> tiers = new HashSet<>();
    for (int i = 0; i < numOfTiers; i++) {
      tiers.add(AppdynamicsTier.builder().name("tier" + i).id(i).build());
    }

    when(restCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.listTiers(anyString(), anyLong())).thenReturn(restCall);

    for (int i = 0; i < numOfTiers; i++) {
      Call<List<AppdynamicsMetric>> appTxnCall = mock(Call.class);
      List<AppdynamicsMetric> appdynamicsTxns = new ArrayList<>();
      for (int j = i; j < numOfTiers; j++) {
        AppdynamicsMetric appdynamicsTxn =
            AppdynamicsMetric.builder().name("txn" + j).type(AppdynamicsMetricType.folder).build();
        appdynamicsTxns.add(appdynamicsTxn);
      }
      when(appTxnCall.execute()).thenReturn(Response.success(appdynamicsTxns));
      when(appdynamicsRestClient.listMetrices(anyString(), anyLong(), eq(BT_PERFORMANCE_PATH_PREFIX + "tier" + i)))
          .thenReturn(appTxnCall);
      Call<List<AppdynamicsMetric>> externalMetricsCall = mock(Call.class);
      AppdynamicsMetric externalCallMetric =
          AppdynamicsMetric.builder().name("txn-" + i + "-" + EXTERNAL_CALLS + i).build();
      when(externalMetricsCall.execute()).thenReturn(Response.success(Lists.newArrayList(externalCallMetric)));

      when(appdynamicsRestClient.listMetrices(anyString(), anyLong(),
               eq(BT_PERFORMANCE_PATH_PREFIX + "tier" + i + "|"
                   + "txn" + i + "|")))
          .thenReturn(externalMetricsCall);
    }

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(UUID.randomUUID().toString().toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();

    Set<AppdynamicsTier> tierDependencies =
        delegateService.getTierDependencies(appDynamicsConfig, 100, Collections.emptyList());
  }

  @Test
  public void testGetBTs() throws IOException, CloneNotSupportedException {
    Call<List<AppdynamicsTier>> tierRestCall = mock(Call.class);
    AppdynamicsTier tier =
        AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build();
    List<AppdynamicsTier> tiers = Lists.newArrayList(tier);
    when(tierRestCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetric>> btsCall = mock(Call.class);
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
    List<AppdynamicsMetric> tierBTMetrics = delegateService.getTierBTMetrics(appDynamicsConfig, new Random().nextLong(),
        new Random().nextLong(), Collections.emptyList(), apiCallLogWithDummyStateExecution(accountId));
    assertEquals(2, tierBTMetrics.size());
    assertEquals(bts, tierBTMetrics);
  }

  @Test
  public void testGetBTData() throws IOException {
    Call<List<AppdynamicsTier>> tierRestCall = mock(Call.class);
    AppdynamicsTier tier =
        AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build();
    List<AppdynamicsTier> tiers = Lists.newArrayList(tier);
    when(tierRestCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetricData>> btDataCall = mock(Call.class);
    List<AppdynamicsMetricData> btData =
        Lists.newArrayList(AppdynamicsMetricData.builder()
                               .metricId(new Random().nextLong())
                               .frequency(UUID.randomUUID().toString())
                               .metricName(UUID.randomUUID().toString())
                               .metricPath(UUID.randomUUID().toString())
                               .metricValues(Lists.newArrayList(AppdynamicsMetricDataValue.builder()
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
                                       .build()))
                               .build(),
            AppdynamicsMetricData.builder()
                .metricId(new Random().nextLong())
                .frequency(UUID.randomUUID().toString())
                .metricName(UUID.randomUUID().toString())
                .metricPath(UUID.randomUUID().toString())
                .metricValues(Lists.newArrayList(AppdynamicsMetricDataValue.builder()
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
                        .build()))
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
        new Random().nextLong(), generateUuid(), generateUuid(), generateUuid(), new Random().nextInt(),
        Collections.emptyList(), apiCallLogWithDummyStateExecution(accountId));
    assertEquals(2, tierBTMetricData.size());
    assertEquals(btData, tierBTMetricData);
  }

  private String saveAppdynamicsConfig() {
    final AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                                    .accountId(accountId)
                                                    .controllerUrl("https://www.google.com")
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
