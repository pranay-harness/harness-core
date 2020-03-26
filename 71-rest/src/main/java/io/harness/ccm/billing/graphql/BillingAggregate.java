package io.harness.ccm.billing.graphql;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.AliasedObject;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.SqlObject;
import io.harness.ccm.billing.GcpBillingTableSchema;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.datafetcher.billing.QLCCMAggregateOperation;

@Data
@Builder
public class BillingAggregate {
  // ideally, gcp specific constants should be organized in a constant class.
  public static final String BILLING_GCP_COST = "cost";
  public static final String BILLING_GCP_CREDITS = "discount";

  QLCCMAggregateOperation operationType;
  String columnName;
  String alias;

  // convert aggregateFunction from QL context to SQL context
  public SqlObject toFunctionCall() {
    Preconditions.checkNotNull(columnName, "Billing aggregate is missing column name.");
    if (operationType == null || columnName == null) {
      return null;
    }

    FunctionCall functionCall = null;
    switch (operationType) {
      case SUM:
        functionCall = FunctionCall.sum();
        break;
      case AVG:
        functionCall = FunctionCall.avg();
        break;
      case MAX:
        functionCall = FunctionCall.max();
        break;
      case MIN:
        functionCall = FunctionCall.min();
        break;
      default:
        return null; // should throw exception
    }

    // map columnName to db columns
    switch (columnName) {
      case BILLING_GCP_COST:
        functionCall.addColumnParams(GcpBillingTableSchema.cost);
        break;
      case BILLING_GCP_CREDITS:
        functionCall.addColumnParams(GcpBillingTableSchema.creditsAmount);
        break;
      default:
        break;
    }
    alias = String.join("_", operationType.name().toLowerCase(), columnName);
    return AliasedObject.toAliasedObject(functionCall, alias);
  }
}
