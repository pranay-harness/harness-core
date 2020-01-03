package software.wings.service.impl.appdynamics;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PARNIAN;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;
import static software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl.BT_PERFORMANCE_PATH_PREFIX;
import static software.wings.service.impl.appdynamics.AppdynamicsDelegateServiceImpl.EXTERNAL_CALLS;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response.Builder;
import okhttp3.internal.http.RealResponseBody;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.WingsBaseTest;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.RequestExecutor;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.appdynamics.AppdynamicsRestClient;
import software.wings.resources.AppdynamicsResource;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.appdynamics.AppdynamicsMetric.AppdynamicsMetricType;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.service.intfc.security.EncryptionService;

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
@Slf4j
public class AppdynamicsApiTest extends WingsBaseTest {
  @Inject private AppdynamicsResource appdynamicsResource;
  @Inject private AppdynamicsService appdynamicsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;
  @Mock private RequestExecutor requestExecutor;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private AppdynamicsRestClient appdynamicsRestClient;
  @Mock private DelegateLogService delegateLogService;

  private AppdynamicsDelegateServiceImpl delegateService;
  private String accountId;

  @Before
  public void setup() throws IllegalAccessException {
    delegateService = spy(new AppdynamicsDelegateServiceImpl());
    doReturn(appdynamicsRestClient).when(delegateService).getAppdynamicsRestClient(any(AppDynamicsConfig.class));
    when(delegateProxyFactory.get(eq(AppdynamicsDelegateService.class), any(SyncTaskContext.class)))
        .thenReturn(delegateService);
    doNothing().when(delegateLogService).save(anyString(), any(ThirdPartyApiCallLog.class));

    FieldUtils.writeField(appdynamicsService, "delegateProxyFactory", delegateProxyFactory, true);
    FieldUtils.writeField(appdynamicsResource, "appdynamicsService", appdynamicsService, true);
    FieldUtils.writeField(delegateService, "encryptionService", encryptionService, true);
    FieldUtils.writeField(delegateService, "delegateLogService", delegateLogService, true);
    FieldUtils.writeField(delegateService, "requestExecutor", requestExecutor, true);
    accountId = UUID.randomUUID().toString();
  }

  @Test
  @Owner(developers = RAGHU, intermittent = true)
  @Category(UnitTests.class)
  public void testUnreachableAppdynamicsServer() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    RuntimeException runtimeException = new RuntimeException(UUID.randomUUID().toString());
    when(restCall.execute()).thenThrow(runtimeException);
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());

    Throwable thrown =
        catchThrowable(() -> appdynamicsService.validateConfig(settingAttribute, Collections.emptyList()));
    assertThat(thrown).isInstanceOf(WingsException.class);
    assertThat(((WingsException) thrown).getCode()).isEqualTo(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR);
    assertThat(((WingsException) thrown).getParams().get("reason"))
        .isEqualTo("Could not reach AppDynamics server. " + ExceptionUtils.getMessage(runtimeException));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNullApplicationName() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    when(restCall.execute())
        .thenReturn(
            Response.success(Lists.newArrayList(NewRelicApplication.builder().id(123).name(generateUuid()).build(),
                NewRelicApplication.builder().id(345).build())));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    final List<NewRelicApplication> applications = appdynamicsService.getApplications(savedAttributeId);
    assertThat(applications.size()).isEqualTo(1);
    assertThat(applications.get(0).getId()).isEqualTo(123);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testInvalidCredential() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    when(restCall.execute())
        .thenReturn(Response.error(new RealResponseBody("Invalid credential", 0, null),
            new Builder()
                .code(HttpStatus.SC_UNAUTHORIZED)
                .request(new Request.Builder().url("https://app.harness.io").build())
                .protocol(Protocol.HTTP_1_1)
                .message("Invalid Credential")
                .build()));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    try {
      appdynamicsService.validateConfig(settingAttribute, Collections.emptyList());
      fail("Validated invalid config");
    } catch (WingsException e) {
      assertThat(e.getCode()).isEqualTo(ErrorCode.APPDYNAMICS_CONFIGURATION_ERROR);
      logger.info("got exception", e);
      assertThat(e.getParams().get("reason"))
          .isEqualTo("Could not login to AppDynamics server with the given credentials");
    }
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testValidConfig() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    when(restCall.execute()).thenReturn(Response.success(Collections.emptyList()));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();
    SettingAttribute settingAttribute = wingsPersistence.get(SettingAttribute.class, savedAttributeId);
    ((AppDynamicsConfig) settingAttribute.getValue()).setPassword(UUID.randomUUID().toString().toCharArray());
    assertThat(appdynamicsService.validateConfig(settingAttribute, Collections.emptyList())).isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
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
    assertThat(allApplications.getResponseMessages().isEmpty()).isTrue();
    assertThat(allApplications.getResource()).isEqualTo(sortedApplicationsByName);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetTiers() throws IOException {
    Set<AppdynamicsTier> tiers =
        Sets.newHashSet(AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build(),
            AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build());
    when(requestExecutor.executeRequest(any(), any())).thenReturn(tiers);

    String savedAttributeId = saveAppdynamicsConfig();

    RestResponse<Set<AppdynamicsTier>> allTiers =
        appdynamicsResource.getAllTiers(accountId, savedAttributeId, new Random().nextLong());
    assertThat(allTiers.getResponseMessages().isEmpty()).isTrue();
    assertThat(allTiers.getResource()).isEqualTo(tiers);
  }

  @Test
  @Owner(developers = PARNIAN)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
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

    Set<AppdynamicsTier> tierDependencies = delegateService.getTierDependencies(
        appDynamicsConfig, 100, Collections.emptyList(), ThirdPartyApiCallLog.builder().build());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetBTs() throws IOException {
    Call<List<AppdynamicsTier>> tierRestCall = mock(Call.class);
    AppdynamicsTier tier =
        AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(new Random().nextInt()).build();
    List<AppdynamicsTier> tiers = Lists.newArrayList(tier);
    when(requestExecutor.executeRequest(any(), eq(tierRestCall))).thenReturn(tiers);
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetric>> btsCall = mock(Call.class);
    List<AppdynamicsMetric> bts = Lists.newArrayList(
        AppdynamicsMetric.builder().name(UUID.randomUUID().toString()).type(AppdynamicsMetricType.leaf).build(),
        AppdynamicsMetric.builder().name(UUID.randomUUID().toString()).type(AppdynamicsMetricType.leaf).build());
    when(btsCall.execute()).thenReturn(Response.success(bts));
    when(btsCall.request()).thenReturn(new Request.Builder().url("https://google.com").build());
    when(appdynamicsRestClient.listMetrices(anyString(), anyLong(), anyString())).thenReturn(btsCall);

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(UUID.randomUUID().toString().toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();
    List<AppdynamicsMetric> tierBTMetrics = delegateService.getTierBTMetrics(appDynamicsConfig, new Random().nextLong(),
        new Random().nextLong(), Collections.emptyList(), createApiCallLog(accountId, null));
    assertThat(tierBTMetrics).hasSize(2);
    assertThat(tierBTMetrics).isEqualTo(bts);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
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
    when(btDataCall.request()).thenReturn(new Request.Builder().url("http://harness-test.appd.com").build());
    when(btDataCall.execute()).thenReturn(Response.success(btData));
    when(appdynamicsRestClient.getMetricDataTimeRange(anyString(), anyLong(), anyString(), anyLong(), anyLong()))
        .thenReturn(btDataCall);

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(UUID.randomUUID().toString().toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();
    List<AppdynamicsMetricData> tierBTMetricData =
        delegateService.getTierBTMetricData(appDynamicsConfig, new Random().nextLong(), generateUuid(), generateUuid(),
            generateUuid(), System.currentTimeMillis() - new Random().nextInt(), System.currentTimeMillis(),
            Collections.emptyList(), createApiCallLog(accountId, null));
    assertThat(tierBTMetricData).hasSize(2);
    assertThat(tierBTMetricData).isEqualTo(btData);
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
                                            .withCategory(SettingCategory.CONNECTOR)
                                            .withEnvId(UUID.randomUUID().toString())
                                            .withName(UUID.randomUUID().toString())
                                            .build();

    return wingsPersistence.save(settingAttribute);
  }
}
