package io.harness.batch.processing.writer;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.integration.EcsEventGenerator;
import io.harness.category.element.UnitTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

@RunWith(MockitoJUnitRunner.class)
public class EcsUtilizationMetricsWriterTest extends CategoryTest implements EcsEventGenerator {
  @InjectMocks private EcsUtilizationMetricsWriter ecsUtilizationMetricsWriter;
  @Mock private UtilizationDataServiceImpl utilizationDataService;

  private final String TEST_ACCOUNT_ID = "ACCOUNT_ID_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_NAME = "CLUSTER_NAME_" + this.getClass().getSimpleName();
  private final String TEST_CLUSTER_ARN = "CLUSTER_ARN_" + this.getClass().getSimpleName();
  private final String TEST_SERVICE_NAME = "SERVICE_NAME_" + this.getClass().getSimpleName();
  private final String TEST_SERVICE_ARN = "SERVICE_ARN_" + this.getClass().getSimpleName();

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteTaskInfo() {
    PublishedMessage ecsUtilizationMetricsMessages = getEcsUtilizationMetricsMessage(
        TEST_ACCOUNT_ID, TEST_CLUSTER_NAME, TEST_CLUSTER_ARN, TEST_SERVICE_NAME, TEST_SERVICE_ARN);

    ecsUtilizationMetricsWriter.write(Arrays.asList(ecsUtilizationMetricsMessages));
    ArgumentCaptor<InstanceUtilizationData> instanceUtilizationDataArgumentCaptor =
        ArgumentCaptor.forClass(InstanceUtilizationData.class);
    verify(utilizationDataService).create(instanceUtilizationDataArgumentCaptor.capture());
    InstanceUtilizationData instanceUtilizationData = instanceUtilizationDataArgumentCaptor.getValue();
    assertThat(instanceUtilizationData.getClusterArn()).isEqualTo(TEST_CLUSTER_ARN);
  }
}
