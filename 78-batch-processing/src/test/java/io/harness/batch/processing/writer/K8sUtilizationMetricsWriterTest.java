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
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.service.intfc.InstanceDataService;
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
  @Mock private InstanceDataService instanceDataService;
  @Mock private JobParameters parameters;

  private final String SETTINGID = "SETTING_ID_" + this.getClass().getSimpleName();
  private final String INSTANCEID = "INSTANCEID" + this.getClass().getSimpleName();
  private final String ACCOUNTID = "ACCOUNTID" + this.getClass().getSimpleName();
  private final String INSTANCETYPE = K8S_POD;
  private final double CPUMAX = 2;
  private final double MEMORYMAX = 1024;
  private final double CPUAVG = 2;
  private final double MEMORYAVG = 1024;
  private final double MEMORYTOTAL = 2048;
  private final double CPUTOTAL = 8;
  private final Instant NOW = Instant.now();
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));
    when(instanceDataService.fetchInstanceDataWithName(ACCOUNTID, SETTINGID, INSTANCEID, START_TIME_MILLIS))
        .thenReturn(InstanceData.builder()
                        .instanceId(INSTANCEID)
                        .totalResource(Resource.builder().cpuUnits(CPUTOTAL).memoryMb(MEMORYTOTAL).build())
                        .build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteK8sUtilizationMetrics() {
    Map<String, InstanceUtilizationData> aggregatedDataMap = new HashMap<>();
    aggregatedDataMap.put(INSTANCEID,
        InstanceUtilizationData.builder()
            .accountId(ACCOUNTID)
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
    assertThat(instanceUtilizationData.getCpuUtilizationAvg()).isEqualTo(CPUAVG / CPUTOTAL);
    assertThat(instanceUtilizationData.getCpuUtilizationMax()).isEqualTo(CPUMAX / CPUTOTAL);
    assertThat(instanceUtilizationData.getMemoryUtilizationAvg()).isEqualTo(MEMORYAVG / MEMORYTOTAL);
    assertThat(instanceUtilizationData.getMemoryUtilizationMax()).isEqualTo(MEMORYMAX / MEMORYTOTAL);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteK8sUtilizationMetricsWhenResourceIsZero() {
    when(instanceDataService.fetchInstanceDataWithName(ACCOUNTID, SETTINGID, INSTANCEID, START_TIME_MILLIS))
        .thenReturn(InstanceData.builder()
                        .instanceId(INSTANCEID)
                        .totalResource(Resource.builder().cpuUnits(0.0).memoryMb(0.0).build())
                        .build());

    Map<String, InstanceUtilizationData> aggregatedDataMap = new HashMap<>();
    aggregatedDataMap.put(INSTANCEID,
        InstanceUtilizationData.builder()
            .accountId(ACCOUNTID)
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
    assertThat(instanceUtilizationData.getCpuUtilizationAvg()).isEqualTo(1);
    assertThat(instanceUtilizationData.getCpuUtilizationMax()).isEqualTo(1);
    assertThat(instanceUtilizationData.getMemoryUtilizationAvg()).isEqualTo(1);
    assertThat(instanceUtilizationData.getMemoryUtilizationMax()).isEqualTo(1);
  }
}
