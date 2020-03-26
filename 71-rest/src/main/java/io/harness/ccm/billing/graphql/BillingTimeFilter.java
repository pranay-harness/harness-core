package io.harness.ccm.billing.graphql;

import static io.harness.ccm.billing.graphql.GcpBillingFilter.BILLING_GCP_ENDTIME;
import static io.harness.ccm.billing.graphql.GcpBillingFilter.BILLING_GCP_STARTTIME;

import com.google.cloud.Timestamp;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.GcpBillingTableSchema;
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
public class BillingTimeFilter implements Filter {
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
      dbColumn = GcpBillingTableSchema.usageStartTime;
    } else if (variable.equals(BILLING_GCP_ENDTIME)) {
      dbColumn = GcpBillingTableSchema.usageEndTime;
    } else {
      throw new InvalidRequestException("Invalid time filter.");
    }

    Condition condition;
    switch (operator) {
      case AFTER:
        condition = BinaryCondition.greaterThanOrEq(dbColumn, timestamp);
        break;
      case BEFORE:
        condition = BinaryCondition.lessThanOrEq(dbColumn, timestamp);
        break;
      default:
        return null;
    }

    return condition;
  }
}
