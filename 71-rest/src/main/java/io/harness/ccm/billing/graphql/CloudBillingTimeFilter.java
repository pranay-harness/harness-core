package io.harness.ccm.billing.graphql;

import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_AWS_STARTTIME;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_GCP_ENDTIME;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_GCP_STARTTIME;

import com.google.cloud.Timestamp;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.RawBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.time.DateUtils;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;

import java.util.Calendar;
import java.util.Date;

@Data
@Builder
public class CloudBillingTimeFilter implements Filter {
  private QLTimeOperator operator;
  private String variable;
  private Number value;

  @Override
  public Number[] getValues() {
    return new Number[] {value};
  }

  public Condition toCondition() {
    Preconditions.checkNotNull(value, "The billing time filter is missing value.");
    Preconditions.checkNotNull(operator, "The billing time filter is missing operator");
    Timestamp timestamp = Timestamp.of(DateUtils.round(new Date(value.longValue()), Calendar.SECOND));

    DbColumn dbColumn = null;
    if (variable.equals(BILLING_GCP_STARTTIME)) {
      dbColumn = PreAggregatedTableSchema.startTime;
    } else if (variable.equals(BILLING_GCP_ENDTIME)) {
      dbColumn = PreAggregatedTableSchema.endTime;
    } else if (variable.equals(BILLING_AWS_STARTTIME)) {
      dbColumn = PreAggregatedTableSchema.startTime;
    } else {
      throw new InvalidRequestException("Invalid time filter.");
    }

    return getCondition(dbColumn, timestamp);
  }

  public Condition toRawTableCondition() {
    Preconditions.checkNotNull(value, "The billing time filter is missing value.");
    Preconditions.checkNotNull(operator, "The billing time filter is missing operator");
    Timestamp timestamp = Timestamp.of(DateUtils.round(new Date(value.longValue()), Calendar.SECOND));

    DbColumn dbColumn = null;
    if (variable.equals(BILLING_GCP_STARTTIME)) {
      dbColumn = RawBillingTableSchema.startTime;
    } else if (variable.equals(BILLING_GCP_ENDTIME)) {
      dbColumn = RawBillingTableSchema.endTime;
    } else if (variable.equals(BILLING_AWS_STARTTIME)) {
      dbColumn = RawBillingTableSchema.startTime;
    } else {
      throw new InvalidRequestException("Invalid time filter.");
    }

    return getCondition(dbColumn, timestamp);
  }

  private Condition getCondition(DbColumn dbColumn, Timestamp timestamp) {
    switch (operator) {
      case AFTER:
        return BinaryCondition.greaterThanOrEq(dbColumn, timestamp);
      case BEFORE:
        return BinaryCondition.lessThanOrEq(dbColumn, timestamp);
      default:
        return null;
    }
  }
}
