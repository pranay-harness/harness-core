package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.K8S_POD;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class K8sUtilizationMetricsWriterTest extends CategoryTest {
  @Inject @InjectMocks K8sUtilizationMetricsWriter k8sUtilizationMetricsWriter;
  @Mock K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;
  @Mock private UtilizationDataServiceImpl utilizationDataService;
  @Mock private JobParameters parameters;

  private final String SETTINGID = "SETTING_ID_" + this.getClass().getSimpleName();
  private final String INSTANCEID = "INSTANCEID" + this.getClass().getSimpleName();
  private final String INSTANCETYPE = K8S_POD;
  private final double CPUMAX = 2;
  private final double MEMORYMAX = 1024;
  private final double CPUAVG = 2;
  private final double MEMORYAVG = 1024;
  private final Instant NOW = Instant.now();
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteK8sUtilizationMetrics() {
    Map<String, InstanceUtilizationData> aggregatedDataMap = new HashMap<>();
    aggregatedDataMap.put("instance1",
        InstanceUtilizationData.builder()
            .settingId(SETTINGID)
            .instanceType(INSTANCETYPE)
            .instanceId(INSTANCEID)
            .cpuUtilizationMax(CPUMAX)
            .cpuUtilizationAvg(CPUAVG)
            .memoryUtilizationMax(MEMORYMAX)
            .memoryUtilizationAvg(MEMORYAVG)
            .build());
    Mockito
        .when(k8sUtilizationGranularDataService.getAggregatedUtilizationData(
            Collections.singletonList(INSTANCEID), START_TIME_MILLIS, END_TIME_MILLIS))
        .thenReturn(aggregatedDataMap);
    k8sUtilizationMetricsWriter.write(Collections.singletonList(Arrays.asList(INSTANCEID)));
    ArgumentCaptor<InstanceUtilizationData> instanceUtilizationDataArgumentCaptor =
        ArgumentCaptor.forClass(InstanceUtilizationData.class);
    verify(utilizationDataService).create(instanceUtilizationDataArgumentCaptor.capture());
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationDataArgumentCaptor.getValue();
    assertThat(instanceUtilizationData.getInstanceId()).isEqualTo(INSTANCEID);
    assertThat(instanceUtilizationData.getInstanceType()).isEqualTo(INSTANCETYPE);
    assertThat(instanceUtilizationData.getSettingId()).isEqualTo(SETTINGID);
    assertThat(instanceUtilizationData.getCpuUtilizationAvg()).isEqualTo(CPUAVG);
    assertThat(instanceUtilizationData.getCpuUtilizationMax()).isEqualTo(CPUMAX);
    assertThat(instanceUtilizationData.getMemoryUtilizationAvg()).isEqualTo(MEMORYAVG);
    assertThat(instanceUtilizationData.getMemoryUtilizationMax()).isEqualTo(MEMORYMAX);
  }
}
