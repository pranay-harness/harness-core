/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import static io.harness.rule.OwnerRule.ROHIT;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.impl.GoogleCloudStorageServiceImpl;
import io.harness.batch.processing.tasklet.support.K8SWorkloadService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.InstanceType;
import io.harness.rule.Owner;
import io.harness.testsupport.BaseTaskletTest;

import software.wings.security.authentication.BatchQueryConfig;

import com.google.common.collect.ImmutableList;
import com.sun.istack.internal.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDataToBigQueryTaskletTest extends BaseTaskletTest {
  public static final String BILLING_DATA = "billing_data";
  public static final int BATCH_SIZE = 500;
  private static final String CLUSTER_ID = "clusterId";
  private static final String NAMESPACE = "namespace";

  @Mock BillingDataServiceImpl billingDataService;
  @Mock private BatchMainConfig config;
  @Mock GoogleCloudStorageServiceImpl googleCloudStorageService;
  @Mock private K8SWorkloadService k8SWorkloadService;
  @InjectMocks ClusterDataToBigQueryTasklet clusterDataToBigQueryTasklet;
  @Mock private ChunkContext chunkContext;
  @Mock private StepContext stepContext;
  @Mock private StepExecution stepExecution;
  @Mock private JobParameters parameters;

  private final Instant END_INSTANT = Instant.now();
  private final Instant START_INSTANT = END_INSTANT.minus(1, ChronoUnit.HOURS);

  @Before
  public void setup() {
    InstanceBillingData instanceBillingData = InstanceBillingData.builder()
                                                  .startTimestamp(START_TIME_MILLIS)
                                                  .endTimestamp(END_TIME_MILLIS)
                                                  .accountId(ACCOUNT_ID)
                                                  .instanceId("instanceId")
                                                  .instanceType("instanceType")
                                                  .billingAmount(BigDecimal.ZERO)
                                                  .cpuBillingAmount(BigDecimal.ZERO)
                                                  .memoryBillingAmount(BigDecimal.ZERO)
                                                  .idleCost(BigDecimal.ZERO)
                                                  .cpuIdleCost(BigDecimal.ZERO)
                                                  .memoryIdleCost(BigDecimal.ZERO)
                                                  .systemCost(BigDecimal.ZERO)
                                                  .cpuSystemCost(BigDecimal.ZERO)
                                                  .memorySystemCost(BigDecimal.ZERO)
                                                  .actualIdleCost(BigDecimal.ZERO)
                                                  .cpuActualIdleCost(BigDecimal.ZERO)
                                                  .memoryActualIdleCost(BigDecimal.ZERO)
                                                  .unallocatedCost(BigDecimal.ZERO)
                                                  .cpuUnallocatedCost(BigDecimal.ZERO)
                                                  .memoryUnallocatedCost(BigDecimal.ZERO)
                                                  .storageBillingAmount(BigDecimal.ZERO)
                                                  .storageActualIdleCost(BigDecimal.ZERO)
                                                  .storageUnallocatedCost(BigDecimal.ZERO)
                                                  .storageUtilizationValue(0D)
                                                  .storageRequest(0D)
                                                  .maxStorageUtilizationValue(0D)
                                                  .maxStorageRequest(0D)
                                                  .orgIdentifier("orgIdentifier")
                                                  .projectIdentifier("projectIdentifier")
                                                  .build();

    when(config.getBatchQueryConfig()).thenReturn(BatchQueryConfig.builder().queryBatchSize(BATCH_SIZE).build());
    when(billingDataService.read(ACCOUNT_ID, Instant.ofEpochMilli(START_TIME_MILLIS),
             Instant.ofEpochMilli(END_TIME_MILLIS), BATCH_SIZE, 0, BatchJobType.CLUSTER_DATA_TO_BIG_QUERY))
        .thenReturn(Collections.singletonList(instanceBillingData));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldExecute() throws Exception {
    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);
    when(parameters.getString(CCMJobConstants.BATCH_JOB_TYPE))
        .thenReturn(BatchJobType.CLUSTER_DATA_TO_BIG_QUERY.name());
    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_INSTANT.toEpochMilli()));
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_INSTANT.toEpochMilli()));
    RepeatStatus execute = clusterDataToBigQueryTasklet.execute(null, chunkContext);
    assertThat(execute).isNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testRefreshLabelCache() {
    final List<InstanceBillingData> dataNotPresentInLabelsCache =
        ImmutableList.of(createBillingData("name0"), createBillingData("name1"));

    when(k8SWorkloadService.getK8sWorkloadLabel(any(), any(), any(), any())).thenReturn(null);

    clusterDataToBigQueryTasklet.refreshLabelCache(ACCOUNT_ID, dataNotPresentInLabelsCache);

    ArgumentCaptor<Set<String>> listArgumentCaptor = ArgumentCaptor.forClass((Class<Set<String>>) (Class) Set.class);
    ArgumentCaptor<K8SWorkloadService.CacheKey> keyArgumentCaptor =
        ArgumentCaptor.forClass(K8SWorkloadService.CacheKey.class);

    verify(k8SWorkloadService, times(1))
        .updateK8sWorkloadLabelCache(keyArgumentCaptor.capture(), listArgumentCaptor.capture());

    assertThat(listArgumentCaptor.getValue()).containsExactlyInAnyOrder("name0", "name1");
    assertThat(keyArgumentCaptor.getValue())
        .isEqualTo(new K8SWorkloadService.CacheKey(ACCOUNT_ID, CLUSTER_ID, NAMESPACE, null));
  }

  private InstanceBillingData createBillingData(@NotNull String name) {
    return InstanceBillingData.builder()
        .accountId(ACCOUNT_ID)
        .clusterId(CLUSTER_ID)
        .instanceType(InstanceType.K8S_POD.name())
        .namespace(NAMESPACE)
        .workloadName(name)
        .build();
  }
}
