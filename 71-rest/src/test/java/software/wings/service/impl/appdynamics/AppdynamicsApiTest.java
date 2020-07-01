package software.wings.service.impl.appdynamics;

import static io.harness.cvng.core.services.CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.QUALITY_PACK_IDENTIFIER;
import static io.harness.cvng.core.services.CVNextGenConstants.RESOURCE_PACK_IDENTIFIER;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.ThirdPartyApiCallLog.createApiCallLog;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.ThirdPartyApiResponseStatus;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.serializer.YamlUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
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
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.DataCollectionException;
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
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by rsingh on 4/24/18.
 */
@Slf4j
public class AppdynamicsApiTest extends WingsBaseTest {
  private static final SecureRandom random = new SecureRandom();

  @Inject private AppdynamicsResource appdynamicsResource;
  @Inject private AppdynamicsService appdynamicsService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EncryptionService encryptionService;
  @Inject private RequestExecutor requestExecutor;
  @Inject private DataCollectionExecutorService dataCollectionService;
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
    FieldUtils.writeField(delegateService, "dataCollectionService", dataCollectionService, true);
    accountId = "TrPOn4AoS022KD8HzSDefA";
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testNullApplicationName() throws IOException {
    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    handleClone(restCall);
    List<NewRelicApplication> allApplications =
        Lists.newArrayList(NewRelicApplication.builder().id(123).name(generateUuid()).build(),
            NewRelicApplication.builder().id(345).build());
    when(restCall.execute()).thenReturn(Response.success(allApplications));
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
  public void testGetApplications() throws IOException {
    List<NewRelicApplication> applications = Lists.newArrayList(
        NewRelicApplication.builder().name(UUID.randomUUID().toString()).id(random.nextInt()).build(),
        NewRelicApplication.builder().name(UUID.randomUUID().toString()).id(random.nextInt()).build());

    List<NewRelicApplication> sortedApplicationsByName =
        applications.stream().sorted(Comparator.comparing(NewRelicApplication::getName)).collect(Collectors.toList());

    String savedAttributeId = saveAppdynamicsConfig();

    Call<List<NewRelicApplication>> restCall = mock(Call.class);
    handleClone(restCall);
    when(restCall.execute()).thenReturn(Response.success(applications));
    when(appdynamicsRestClient.listAllApplications(anyString())).thenReturn(restCall);
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
        Sets.newHashSet(AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(random.nextInt()).build(),
            AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(random.nextInt()).build());
    Call<Set<AppdynamicsTier>> restCall = mock(Call.class);
    handleClone(restCall);
    when(restCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.listTiers(anyString(), anyLong())).thenReturn(restCall);

    String savedAttributeId = saveAppdynamicsConfig();

    RestResponse<Set<AppdynamicsTier>> allTiers =
        appdynamicsResource.getAllTiers(accountId, savedAttributeId, random.nextLong());
    assertThat(allTiers.getResponseMessages().isEmpty()).isTrue();
    assertThat(allTiers.getResource()).isEqualTo(tiers);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetBTs() throws IOException {
    Call<List<AppdynamicsTier>> tierRestCall = mock(Call.class);
    handleClone(tierRestCall);
    when(tierRestCall.execute())
        .thenReturn(Response.success(Lists.newArrayList(
            AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(random.nextInt()).build())));
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetric>> btsCall = mock(Call.class);
    handleClone(btsCall);
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
    List<AppdynamicsMetric> tierBTMetrics = delegateService.getTierBTMetrics(appDynamicsConfig, random.nextLong(),
        random.nextLong(), Collections.emptyList(), createApiCallLog(accountId, null));
    assertThat(tierBTMetrics).hasSize(2);
    assertThat(tierBTMetrics).isEqualTo(bts);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetBTData() throws IOException {
    Call<List<AppdynamicsTier>> tierRestCall = mock(Call.class);
    handleClone(tierRestCall);
    AppdynamicsTier tier = AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(random.nextInt()).build();
    List<AppdynamicsTier> tiers = Lists.newArrayList(tier);
    when(tierRestCall.execute()).thenReturn(Response.success(tiers));
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetricData>> btDataCall = mock(Call.class);
    handleClone(btDataCall);
    List<AppdynamicsMetricData> btData =
        Lists.newArrayList(AppdynamicsMetricData.builder()
                               .metricId(random.nextLong())
                               .frequency(UUID.randomUUID().toString())
                               .metricName(UUID.randomUUID().toString())
                               .metricPath(UUID.randomUUID().toString())
                               .metricValues(Lists.newArrayList(AppdynamicsMetricDataValue.builder()
                                                                    .count(random.nextLong())
                                                                    .current(random.nextLong())
                                                                    .max(random.nextLong())
                                                                    .min(random.nextLong())
                                                                    .occurrences(random.nextInt())
                                                                    .standardDeviation(random.nextDouble())
                                                                    .sum(random.nextLong())
                                                                    .startTimeInMillis(random.nextLong())
                                                                    .useRange(random.nextBoolean())
                                                                    .value(random.nextDouble())
                                                                    .build(),
                                   AppdynamicsMetricDataValue.builder()
                                       .count(random.nextLong())
                                       .current(random.nextLong())
                                       .max(random.nextLong())
                                       .min(random.nextLong())
                                       .occurrences(random.nextInt())
                                       .standardDeviation(random.nextDouble())
                                       .sum(random.nextLong())
                                       .startTimeInMillis(random.nextLong())
                                       .useRange(random.nextBoolean())
                                       .value(random.nextDouble())
                                       .build()))
                               .build(),
            AppdynamicsMetricData.builder()
                .metricId(random.nextLong())
                .frequency(UUID.randomUUID().toString())
                .metricName(UUID.randomUUID().toString())
                .metricPath(UUID.randomUUID().toString())
                .metricValues(Lists.newArrayList(AppdynamicsMetricDataValue.builder()
                                                     .count(random.nextLong())
                                                     .current(random.nextLong())
                                                     .max(random.nextLong())
                                                     .min(random.nextLong())
                                                     .occurrences(random.nextInt())
                                                     .standardDeviation(random.nextDouble())
                                                     .sum(random.nextLong())
                                                     .startTimeInMillis(random.nextLong())
                                                     .useRange(random.nextBoolean())
                                                     .value(random.nextDouble())
                                                     .build(),
                    AppdynamicsMetricDataValue.builder()
                        .count(random.nextLong())
                        .current(random.nextLong())
                        .max(random.nextLong())
                        .min(random.nextLong())
                        .occurrences(random.nextInt())
                        .standardDeviation(random.nextDouble())
                        .sum(random.nextLong())
                        .startTimeInMillis(random.nextLong())
                        .useRange(random.nextBoolean())
                        .value(random.nextDouble())
                        .build()))
                .build());
    when(btDataCall.request()).thenReturn(new Request.Builder().url("http://harness-test.appd.com").build());
    when(btDataCall.execute()).thenReturn(Response.success(btData));
    when(appdynamicsRestClient.getMetricDataTimeRange(
             anyString(), anyLong(), anyString(), anyLong(), anyLong(), anyBoolean()))
        .thenReturn(btDataCall);

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .accountId(accountId)
                                              .controllerUrl(UUID.randomUUID().toString())
                                              .username(UUID.randomUUID().toString())
                                              .password(UUID.randomUUID().toString().toCharArray())
                                              .accountname(UUID.randomUUID().toString())
                                              .build();
    List<AppdynamicsMetricData> tierBTMetricData =
        delegateService.getTierBTMetricData(appDynamicsConfig, random.nextLong(), generateUuid(), generateUuid(),
            generateUuid(), System.currentTimeMillis() - random.nextInt(), System.currentTimeMillis(),
            Collections.emptyList(), createApiCallLog(accountId, null));
    assertThat(tierBTMetricData).hasSize(2);
    assertThat(tierBTMetricData).isEqualTo(btData);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetMetricPackData() throws IOException {
    Call<List<AppdynamicsTier>> tierRestCall = mock(Call.class);
    handleClone(tierRestCall);
    when(tierRestCall.execute())
        .thenReturn(Response.success(Lists.newArrayList(
            AppdynamicsTier.builder().name(UUID.randomUUID().toString()).id(random.nextInt()).build())));
    when(appdynamicsRestClient.getTierDetails(anyString(), anyLong(), anyLong())).thenReturn(tierRestCall);

    Call<List<AppdynamicsMetricData>> metricDataSuccessCall = mock(Call.class);
    handleClone(metricDataSuccessCall);
    when(metricDataSuccessCall.execute())
        .thenReturn(Response.success(
            Lists.newArrayList(AppdynamicsMetricData.builder()
                                   .metricName(generateUuid())
                                   .metricValues(Lists.newArrayList(
                                       AppdynamicsMetricDataValue.builder().value(random.nextDouble()).build()))
                                   .build())));
    Call<List<AppdynamicsMetricData>> metricDataNoDataCall = mock(Call.class);
    handleClone(metricDataNoDataCall);
    when(metricDataNoDataCall.execute())
        .thenReturn(Response.success(Lists.newArrayList(
            AppdynamicsMetricData.builder().metricName(generateUuid()).metricValues(Lists.newArrayList()).build())));

    Call<List<AppdynamicsMetricData>> metricDataErrorCall = mock(Call.class);
    handleClone(metricDataErrorCall);
    doThrow(new DataCollectionException("invalid request")).when(metricDataErrorCall).execute();
    when(appdynamicsRestClient.getMetricDataTimeRange(
             anyString(), anyLong(), anyString(), anyLong(), anyLong(), anyBoolean()))
        .thenAnswer(invocationOnMock -> {
          final String metricPath = invocationOnMock.getArgumentAt(2, String.class);
          if (metricPath.contains("Exceptions")) {
            return metricDataNoDataCall;
          }
          if (metricPath.contains("CPU")) {
            return metricDataErrorCall;
          }
          return metricDataSuccessCall;
        });
    YamlUtils yamlUtils = new YamlUtils();
    final String metricPackYaml = Resources.toString(
        AppdynamicsApiTest.class.getResource("/appdynamics/app-dynamics-metric-packs-list.yaml"), Charsets.UTF_8);
    final List<MetricPackDTO> metricPacks = yamlUtils.read(metricPackYaml, new TypeReference<List<MetricPackDTO>>() {});

    final Set<AppdynamicsValidationResponse> metricPacksData = appdynamicsService.getMetricPackData(
        accountId, generateUuid(), saveAppdynamicsConfig(), 100, 200, generateUuid(), metricPacks);
    assertThat(metricPacksData.size()).isEqualTo(metricPacks.size());
    metricPacksData.forEach(metricPackData -> {
      if (metricPackData.getMetricPackName().equals(PERFORMANCE_PACK_IDENTIFIER)) {
        assertThat(metricPackData.getOverallStatus()).isEqualTo(ThirdPartyApiResponseStatus.SUCCESS);
      }

      if (metricPackData.getMetricPackName().equals(QUALITY_PACK_IDENTIFIER)) {
        assertThat(metricPackData.getOverallStatus()).isEqualTo(ThirdPartyApiResponseStatus.NO_DATA);
      }

      if (metricPackData.getMetricPackName().equals(RESOURCE_PACK_IDENTIFIER)) {
        assertThat(metricPackData.getOverallStatus()).isEqualTo(ThirdPartyApiResponseStatus.FAILED);
        metricPackData.getValues().forEach(response -> {
          if (response.getMetricName().contains("CPU")) {
            assertThat(response.getApiResponseStatus()).isEqualTo(ThirdPartyApiResponseStatus.FAILED);
            assertThat(response.getErrorMessage())
                .isEqualTo("DataCollectionException: " + DataCollectionException.class.getName() + ": invalid request");
          }
        });
      }
    });
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

  private void handleClone(Call restCall) {
    when(restCall.clone()).thenReturn(restCall);
    when(restCall.request()).thenReturn(new Request.Builder().url("https://google.com").build());
  }
}
