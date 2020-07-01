package io.harness.perpetualtask.datacollection;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.AppDynamicsDataCollectionInfo;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.beans.MetricPackDTO.MetricDefinitionDTO;
import io.harness.cvng.core.services.CVNextGenConstants;
import io.harness.cvng.perpetualtask.CVDataCollectionInfo;
import io.harness.data.structure.CollectionUtils;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.delegate.service.TimeSeriesDataStoreService;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.serializer.KryoUtils;
import io.harness.verificationclient.CVNextGenServiceClient;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.AppDynamicsConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class DataCollectionPerpetualTaskExecutorTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  private DataCollectionPerpetualTaskExecutor dataCollector = new DataCollectionPerpetualTaskExecutor();
  @Mock private TimeSeriesDataStoreService timeSeriesDataStoreService;
  @Mock private EncryptionService encryptionService;
  @Mock private DataCollectionDSLService dataCollectionDSLService;
  @Mock private CVNextGenServiceClient cvNextGenServiceClient;
  private AppDynamicsConfig appDynamicsConfig;
  private String accountId;
  private String cvConfigId;
  private DataCollectionTaskDTO dataCollectionTaskDTO;
  private AppDynamicsDataCollectionInfo dataCollectionInfo;

  private PerpetualTaskExecutionParams perpetualTaskParams;

  @Before
  public void setup() throws IllegalAccessException, IOException {
    accountId = generateUuid();
    cvConfigId = generateUuid();

    appDynamicsConfig = AppDynamicsConfig.builder()
                            .controllerUrl(generateUuid())
                            .username(generateUuid())
                            .password(generateUuid().toCharArray())
                            .build();
    appDynamicsConfig.setDecrypted(true);

    FieldUtils.writeField(dataCollector, "encryptionService", encryptionService, true);
    FieldUtils.writeField(dataCollector, "timeSeriesDataStoreService", timeSeriesDataStoreService, true);
    FieldUtils.writeField(dataCollector, "dataCollectionDSLService", dataCollectionDSLService, true);
    FieldUtils.writeField(dataCollector, "cvNextGenServiceClient", cvNextGenServiceClient, true);
    dataCollectionInfo = AppDynamicsDataCollectionInfo.builder().applicationId(123).tierId(1234).build();
    dataCollectionTaskDTO =
        DataCollectionTaskDTO.builder().accountId(accountId).dataCollectionInfo(dataCollectionInfo).build();
    Call<RestResponse<DataCollectionTaskDTO>> nextTaskCall = mock(Call.class);
    when(nextTaskCall.execute()).thenReturn(Response.success(new RestResponse<>(dataCollectionTaskDTO)));
    when(cvNextGenServiceClient.getNextDataCollectionTask(anyString(), anyString())).thenReturn(nextTaskCall);
    Call<RestResponse<Void>> taskUpdateResult = mock(Call.class);
    when(cvNextGenServiceClient.updateTaskStatus(anyString(), any())).thenReturn(taskUpdateResult);
  }

  private void createTaskParams(String metricPackIdentifier, String dataCollectionDsl) {
    dataCollectionInfo.setMetricPack(
        MetricPackDTO.builder()
            .identifier(metricPackIdentifier)
            .metrics(
                Sets.newHashSet(MetricDefinitionDTO.builder().name(generateUuid()).path("path1").included(true).build(),
                    MetricDefinitionDTO.builder().name(generateUuid()).path("path2").included(false).build(),
                    MetricDefinitionDTO.builder().name(generateUuid()).path("path3").included(true).build()))
            .build());
    dataCollectionInfo.setDataCollectionDsl(dataCollectionDsl);
    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .settingValue(appDynamicsConfig)
                                                    .encryptedDataDetails(Lists.newArrayList())
                                                    .build();
    ByteString bytes = ByteString.copyFrom(KryoUtils.asBytes(cvDataCollectionInfo));
    perpetualTaskParams = PerpetualTaskExecutionParams.newBuilder()
                              .setCustomizedParams(Any.pack(DataCollectionPerpetualTaskParams.newBuilder()
                                                                .setAccountId(accountId)
                                                                .setCvConfigId(cvConfigId)
                                                                .setDataCollectionInfo(bytes)
                                                                .build()))
                              .build();
  }

  @Test
  @Owner(developers = OwnerRule.RAGHU)
  @Category({UnitTests.class})
  public void testDataCollection_executeDSL() {
    createTaskParams(CVNextGenConstants.PERFORMANCE_PACK_IDENTIFIER, "dsl");
    dataCollector.runOnce(PerpetualTaskId.newBuilder().build(), perpetualTaskParams, Instant.now());
    verifyDsl("dsl");
  }

  private void verifyDsl(String dsl) {
    ArgumentCaptor<String> dslCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<RuntimeParameters> runtimeParams = ArgumentCaptor.forClass(RuntimeParameters.class);
    ArgumentCaptor<ThirdPartyCallHandler> apiCallHandler = ArgumentCaptor.forClass(ThirdPartyCallHandler.class);

    verify(dataCollectionDSLService).execute(dslCaptor.capture(), runtimeParams.capture(), apiCallHandler.capture());

    assertThat(dsl).isNotEmpty();
    assertThat(dslCaptor.getValue()).isEqualTo(dsl);
    RuntimeParameters runtimeParameters = runtimeParams.getValue();
    assertThat(runtimeParameters.getBaseUrl()).isEqualTo(appDynamicsConfig.getControllerUrl() + "/");
    assertThat(runtimeParameters.getCommonHeaders().size()).isEqualTo(1);
    assertThat(runtimeParameters.getCommonHeaders().get("Authorization"))
        .isEqualTo("Basic "
            + Base64.encodeBase64String(
                  String
                      .format("%s@%s:%s", appDynamicsConfig.getUsername(), appDynamicsConfig.getAccountname(),
                          new String(appDynamicsConfig.getPassword()))
                      .getBytes(StandardCharsets.UTF_8)));
    Map<String, Object> otherEnvVariables = runtimeParameters.getOtherEnvVariables();
    assertThat(otherEnvVariables.size()).isEqualTo(3);
    assertThat(otherEnvVariables.get("appId")).isEqualTo(dataCollectionInfo.getApplicationId());
    assertThat(otherEnvVariables.get("tierId")).isEqualTo(dataCollectionInfo.getTierId());

    List<String> metricsToCollect = (List<String>) otherEnvVariables.get("metricsToCollect");
    assertThat(CollectionUtils.isEqualCollection(metricsToCollect, Lists.newArrayList("path1", "path3"))).isTrue();
  }
}
