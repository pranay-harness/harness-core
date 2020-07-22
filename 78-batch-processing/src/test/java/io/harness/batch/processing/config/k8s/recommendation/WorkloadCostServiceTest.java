package io.harness.batch.processing.config.k8s.recommendation;

import static io.harness.batch.processing.config.k8s.recommendation.WorkloadCostService.WORKLOAD_COST_QUERY;
import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.config.k8s.recommendation.WorkloadCostService.Cost;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

@RunWith(MockitoJUnitRunner.class)
public class WorkloadCostServiceTest extends CategoryTest {
  @InjectMocks private WorkloadCostService workloadCostService;
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock private PreparedStatement statement;
  @Mock private Connection connection;
  @Mock private ResultSet resultSet;

  @Before
  public void setUp() throws Exception {
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    when(timeScaleDBService.isValid()).thenReturn(true);
    when(connection.prepareStatement(WORKLOAD_COST_QUERY)).thenReturn(statement);
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkloadCost() throws Exception {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    BigDecimal cpuCost = new BigDecimal("4.35");
    when(resultSet.getBigDecimal(1)).thenReturn(cpuCost);
    BigDecimal memoryCost = new BigDecimal("3.12");
    when(resultSet.getBigDecimal(2)).thenReturn(memoryCost);
    ResourceId workloadId = ResourceId.builder()
                                .accountId("29b62881-fd2a-4265-a028-b1015a96b77b")
                                .clusterId("8c8d8e25-e0ba-4727-914c-0d830944ee72")
                                .namespace("harness")
                                .name("manager")
                                .kind("Deployment")
                                .build();
    Instant begin = Instant.EPOCH.plus(Duration.ofHours(1).plusMinutes(3));
    Instant end = Instant.EPOCH.plus(Duration.ofDays(1).plusHours(1).plusMinutes(3));
    Cost actualCost = workloadCostService.getActualCost(workloadId, begin, end);
    assertThat(actualCost).isEqualTo(Cost.builder().cpu(cpuCost).memory(memoryCost).build());
    verify(statement).setString(1, workloadId.getAccountId());
    verify(statement).setString(2, workloadId.getClusterId());
    verify(statement).setString(3, workloadId.getNamespace());
    verify(statement).setString(4, workloadId.getKind());
    verify(statement).setString(5, workloadId.getName());
    verify(statement).setTimestamp(6, new Timestamp(begin.toEpochMilli()));
    verify(statement).setTimestamp(7, new Timestamp(end.toEpochMilli()));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkloadCostException() throws Exception {
    when(statement.executeQuery()).thenThrow(new SQLException("failed"));
    ResourceId workloadId = ResourceId.builder()
                                .accountId("29b62881-fd2a-4265-a028-b1015a96b77b")
                                .clusterId("8c8d8e25-e0ba-4727-914c-0d830944ee72")
                                .namespace("harness")
                                .name("manager")
                                .kind("Deployment")
                                .build();
    Instant begin = Instant.EPOCH.plus(Duration.ofHours(1).plusMinutes(3));
    Instant end = Instant.EPOCH.plus(Duration.ofDays(1).plusHours(1).plusMinutes(3));
    assertThat(workloadCostService.getActualCost(workloadId, begin, end)).isNull();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void testWorkloadCostNoResult() throws Exception {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);
    ResourceId workloadId = ResourceId.builder()
                                .accountId("29b62881-fd2a-4265-a028-b1015a96b77b")
                                .clusterId("8c8d8e25-e0ba-4727-914c-0d830944ee72")
                                .namespace("harness")
                                .name("manager")
                                .kind("Deployment")
                                .build();
    Instant begin = Instant.EPOCH.plus(Duration.ofHours(1).plusMinutes(3));
    Instant end = Instant.EPOCH.plus(Duration.ofDays(1).plusHours(1).plusMinutes(3));
    assertThat(workloadCostService.getActualCost(workloadId, begin, end)).isNull();
  }
}
