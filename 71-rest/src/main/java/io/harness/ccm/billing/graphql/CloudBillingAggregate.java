package io.harness.ccm.billing.graphql;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.AliasedObject;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.RawBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;

@Data
@Builder
public class CloudBillingAggregate {
  // ideally, gcp specific constants should be organized in a constant class.
  public static final String BILLING_GCP_COST = "cost";
  public static final String BILLING_GCP_CREDITS = "discount";
  public static final String AWS_UN_BLENDED_COST = "unblendedCost";
  public static final String AWS_BLENDED_COST = "blendedCost";
  public static final String PRE_AGG_START_TIME = "startTime";

  QLCCMAggregateOperation operationType;
  String columnName;
  String alias;

  // convert aggregateFunction from QL context to SQL context
  public SqlObject toFunctionCall() {
    Preconditions.checkNotNull(columnName, "Billing aggregate is missing column name.");
    if (operationType == null || columnName == null) {
      return null;
    }

    FunctionCall functionCall = getFunctionCallType();

    // map columnName to db columns
    switch (columnName) {
      case BILLING_GCP_COST:
        functionCall.addColumnParams(PreAggregatedTableSchema.cost);
        break;
      case BILLING_GCP_CREDITS:
        functionCall.addColumnParams(PreAggregatedTableSchema.discount);
        break;
      case AWS_UN_BLENDED_COST:
        functionCall.addColumnParams(PreAggregatedTableSchema.awsUnBlendedCost);
        break;
      case AWS_BLENDED_COST:
        functionCall.addColumnParams(PreAggregatedTableSchema.awsBlendedCost);
        break;
      case PRE_AGG_START_TIME:
        functionCall.addColumnParams(PreAggregatedTableSchema.startTime);
        break;
      default:
        break;
    }
    alias = String.join("_", operationType.name().toLowerCase(), columnName);
    return AliasedObject.toAliasedObject(functionCall, alias);
  }

  public SqlObject toRawTableFunctionCall() {
    Preconditions.checkNotNull(columnName, "Billing aggregate is missing column name.");
    if (operationType == null || columnName == null) {
      return null;
    }

    FunctionCall functionCall = getFunctionCallType();
    switch (columnName) {
      case BILLING_GCP_COST:
        functionCall.addColumnParams(RawBillingTableSchema.cost);
        break;
      case PRE_AGG_START_TIME:
        functionCall.addColumnParams(RawBillingTableSchema.startTime);
        break;
      default:
        break;
    }
    alias = String.join("_", operationType.name().toLowerCase(), columnName);
    return AliasedObject.toAliasedObject(functionCall, alias);
  }

  private FunctionCall getFunctionCallType() {
    switch (operationType) {
      case SUM:
        return FunctionCall.sum();
      case AVG:
        return FunctionCall.avg();
      case MAX:
        return FunctionCall.max();
      case MIN:
        return FunctionCall.min();
      default:
        return null;
    }
  }
}
