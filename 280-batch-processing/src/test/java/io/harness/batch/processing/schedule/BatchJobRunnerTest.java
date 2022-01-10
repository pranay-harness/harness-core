/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.schedule;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.service.intfc.BatchJobScheduledDataService;
import io.harness.batch.processing.service.intfc.CustomBillingMetaDataService;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BatchJobRunnerTest extends CategoryTest {
  @InjectMocks private BatchJobRunner batchJobRunner;
  @Mock private BatchJobScheduledDataService batchJobScheduledDataService;
  @Mock private CustomBillingMetaDataService customBillingMetaDataService;

  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final Instant NOW = Instant.now();
  private final Instant START_TIME = NOW.minus(1, ChronoUnit.HOURS);
  private final Instant END_TIME = NOW;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfAllDependentJobFinished() {
    when(batchJobScheduledDataService.fetchLastDependentBatchJobScheduledTime(ACCOUNT_ID, BatchJobType.K8S_UTILIZATION))
        .thenReturn(NOW.minus(2, ChronoUnit.DAYS));
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished = batchJobRunner.checkDependentJobFinished(
        ACCOUNT_ID, NOW.minus(3, ChronoUnit.DAYS), batchJobType.getDependentBatchJobs());
    assertThat(jobFinished).isTrue();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfAllDependentJobNotFinished() {
    when(batchJobScheduledDataService.fetchLastDependentBatchJobScheduledTime(ACCOUNT_ID, BatchJobType.K8S_UTILIZATION))
        .thenReturn(NOW.minus(4, ChronoUnit.DAYS));
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished = batchJobRunner.checkDependentJobFinished(
        ACCOUNT_ID, NOW.minus(3, ChronoUnit.DAYS), batchJobType.getDependentBatchJobs());
    assertThat(jobFinished).isFalse();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfOutOfClusterDependentJobsNotFinished() {
    when(customBillingMetaDataService.checkPipelineJobFinished(ACCOUNT_ID, START_TIME, END_TIME))
        .thenReturn(Boolean.FALSE);
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished = batchJobRunner.checkOutOfClusterDependentJobs(ACCOUNT_ID, START_TIME, END_TIME, batchJobType);
    assertThat(jobFinished).isFalse();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfOutOfClusterDependentJobFinished() {
    when(customBillingMetaDataService.checkPipelineJobFinished(ACCOUNT_ID, START_TIME, END_TIME))
        .thenReturn(Boolean.TRUE);
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished = batchJobRunner.checkOutOfClusterDependentJobs(ACCOUNT_ID, START_TIME, END_TIME, batchJobType);
    assertThat(jobFinished).isTrue();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnTrueIfOutOfClusterDependentJobsIsNotApplicable() {
    BatchJobType batchJobType = BatchJobType.K8S_EVENT;
    boolean jobFinished = batchJobRunner.checkOutOfClusterDependentJobs(ACCOUNT_ID, START_TIME, END_TIME, batchJobType);
    assertThat(jobFinished).isTrue();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnFalseIfDependentJobFinished() {
    when(batchJobScheduledDataService.fetchLastDependentBatchJobScheduledTime(any(), any(BatchJobType.class)))
        .thenReturn(NOW);
    BatchJobType batchJobType = BatchJobType.INSTANCE_BILLING;
    boolean jobFinished =
        batchJobRunner.checkDependentJobFinished(ACCOUNT_ID, NOW, batchJobType.getDependentBatchJobs());
    assertThat(jobFinished).isFalse();
  }
}
