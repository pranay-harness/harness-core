package io.harness.batch.processing.config.k8s.recommendation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;

import io.harness.timescaledb.TimeScaleDBService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class WorkloadCostService {
  @VisibleForTesting
  static final String WORKLOAD_COST_QUERY = "SELECT SUM(CPUBILLINGAMOUNT), SUM(MEMORYBILLINGAMOUNT)"
      + "FROM BILLING_DATA WHERE INSTANCETYPE = 'K8S_POD' AND ACCOUNTID = ? AND CLUSTERID = ? AND NAMESPACE = ? "
      + "AND WORKLOADTYPE = ? AND WORKLOADNAME = ? AND STARTTIME >= ? AND STARTTIME < ?";

  private final TimeScaleDBService timeScaleDBService;

  public WorkloadCostService(TimeScaleDBService timeScaleDBService) {
    this.timeScaleDBService = timeScaleDBService;
  }

  @Value
  @Builder
  static class Cost {
    BigDecimal cpu;
    BigDecimal memory;
  }

  private boolean isTruncatedToDay(Instant instant) {
    return instant.truncatedTo(ChronoUnit.DAYS).equals(instant);
  }

  /**
   * Get actual cost incurred by this workload, in a given period (truncated to days)
   */
  public Cost getActualCost(ResourceId workloadId, Instant begin, Instant end) {
    checkState(timeScaleDBService.isValid());
    checkArgument(isTruncatedToDay(begin));
    checkArgument(isTruncatedToDay(end));
    for (int retryCount = 0; retryCount < 5; retryCount++) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(WORKLOAD_COST_QUERY)) {
        statement.setString(1, workloadId.getAccountId());
        statement.setString(2, workloadId.getClusterId());
        statement.setString(3, workloadId.getNamespace());
        statement.setString(4, workloadId.getKind());
        statement.setString(5, workloadId.getName());
        Timestamp beginTs = new Timestamp(begin.toEpochMilli());
        statement.setTimestamp(6, beginTs);
        Timestamp endTs = new Timestamp(end.toEpochMilli());
        statement.setTimestamp(7, endTs);
        try (val resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            BigDecimal cpuCost = resultSet.getBigDecimal(1);
            BigDecimal memoryCost = resultSet.getBigDecimal(2);
            if (cpuCost == null && memoryCost == null) {
              return null;
            }
            return Cost.builder().cpu(cpuCost).memory(memoryCost).build();
          }
        }
      } catch (SQLException e) {
        logger.error("Failed to fetch cost. retryCount=[{}]", retryCount, e);
      }
    }
    return null;
  }
}
