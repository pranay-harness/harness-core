package io.harness.batch.processing.config.k8s.recommendation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import io.harness.timescaledb.TimeScaleDBService;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WorkloadCostService {
  /**
   *  1,6-accountId
   *  2,7-clusterId
   *  3,8-namespace
   *  4,9-workloadtype
   *  5,10-workloadname
   *  11 - startInclusive
   */
  @VisibleForTesting
  static final String LAST_AVAILABLE_DAY_COST =
      "SELECT SUM(CPUBILLINGAMOUNT) AS CPUBILLINGAMOUNT, SUM(MEMORYBILLINGAMOUNT) AS MEMORYBILLINGAMOUNT, SUM(CPUREQUEST) AS CPUREQUEST, SUM(MEMORYREQUEST) as MEMORYREQUEST, SUM(USAGEDURATIONSECONDS) as USAGEDURATIONSECONDS  "
      + "FROM BILLING_DATA WHERE INSTANCETYPE = 'K8S_POD' AND ACCOUNTID = ? AND CLUSTERID = ? AND NAMESPACE = ? "
      + "AND WORKLOADTYPE = ? AND WORKLOADNAME = ? AND STARTTIME = "
      + "(SELECT MAX(STARTTIME) FROM BILLING_DATA WHERE INSTANCETYPE = 'K8S_POD' AND ACCOUNTID = ? AND CLUSTERID = ? "
      + "AND NAMESPACE = ? AND WORKLOADTYPE = ? AND WORKLOADNAME = ? AND STARTTIME >= ?)";

  private final TimeScaleDBService timeScaleDBService;

  public WorkloadCostService(TimeScaleDBService timeScaleDBService) {
    this.timeScaleDBService = timeScaleDBService;
  }

  @Value
  @Builder
  static class Cost {
    BigDecimal cpu;
    BigDecimal memory;
    // unit cost per seconds
    BigDecimal cpuUnitCost;
    BigDecimal memoryUnitCost;
  }

  private boolean isTruncatedToDay(Instant instant) {
    return instant.truncatedTo(ChronoUnit.DAYS).equals(instant);
  }

  /**
   * Get actual cost incurred by this workload, in the last day for which cost is available.
   */
  public Cost getLastAvailableDayCost(ResourceId workloadId, Instant startInclusive) {
    checkState(timeScaleDBService.isValid());
    checkArgument(isTruncatedToDay(startInclusive));
    for (int retryCount = 0; retryCount < 5; retryCount++) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(LAST_AVAILABLE_DAY_COST)) {
        statement.setString(1, workloadId.getAccountId());
        statement.setString(2, workloadId.getClusterId());
        statement.setString(3, workloadId.getNamespace());
        statement.setString(4, workloadId.getKind());
        statement.setString(5, workloadId.getName());
        statement.setString(6, workloadId.getAccountId());
        statement.setString(7, workloadId.getClusterId());
        statement.setString(8, workloadId.getNamespace());
        statement.setString(9, workloadId.getKind());
        statement.setString(10, workloadId.getName());
        statement.setTimestamp(11, new Timestamp(startInclusive.toEpochMilli()));
        try (val resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            BigDecimal cpuCost = resultSet.getBigDecimal("CPUBILLINGAMOUNT");
            BigDecimal memoryCost = resultSet.getBigDecimal("MEMORYBILLINGAMOUNT");
            BigDecimal cpuRequest = resultSet.getBigDecimal("CPUREQUEST");
            BigDecimal memoryRequest = resultSet.getBigDecimal("MEMORYREQUEST");
            BigDecimal usageDurationSeconds = resultSet.getBigDecimal("USAGEDURATIONSECONDS");

            if (cpuCost == null && memoryCost == null) {
              return null;
            }
            BigDecimal memoryUnitPrice = null;
            BigDecimal cpuUnitPrice = null;

            if (usageDurationSeconds != null && usageDurationSeconds.compareTo(BigDecimal.ZERO) > 0) {
              if (memoryCost != null && memoryRequest != null && memoryRequest.compareTo(BigDecimal.ZERO) > 0) {
                memoryUnitPrice = memoryCost.divide(memoryRequest, MathContext.DECIMAL128)
                                      .divide(usageDurationSeconds, MathContext.DECIMAL128);
              }
              if (cpuCost != null && cpuRequest != null && cpuRequest.compareTo(BigDecimal.ZERO) > 0) {
                cpuUnitPrice = cpuCost.divide(cpuRequest, MathContext.DECIMAL128)
                                   .divide(usageDurationSeconds, MathContext.DECIMAL128);
              }
            }

            return Cost.builder()
                .cpu(cpuCost)
                .memory(memoryCost)
                .cpuUnitCost(cpuUnitPrice)
                .memoryUnitCost(memoryUnitPrice)
                .build();
          }
        }
      } catch (SQLException e) {
        log.error("Failed to fetch cost. retryCount=[{}]", retryCount, e);
      }
    }
    return null;
  }
}
