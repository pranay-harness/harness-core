package io.harness.ccm.billing.graphql;

import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_AWS_INSTANCE_TYPE;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_AWS_LINKED_ACCOUNT;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_AWS_REGION;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_AWS_SERVICE;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_AWS_USAGE_TYPE;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_GCP_BILLING_ACCOUNT_ID;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_GCP_PRODUCT;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_GCP_PROJECT;
import static io.harness.ccm.billing.graphql.OutOfClusterBillingFilter.BILLING_GCP_SKU;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.GcpBillingTableSchema;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

@Data
@Builder
public class BillingIdFilter implements Filter {
  private QLIdOperator operator;
  private String variable;
  private String[] values;

  @Override
  public Object getOperator() {
    return null;
  }

  @Override
  public Object[] getValues() {
    return values.clone();
  }

  public Condition toCondition() {
    Preconditions.checkNotNull(values, "The billing Id filter is missing values");
    Preconditions.checkNotNull(operator, "The billing Id filter is missing operator");

    DbColumn dbColumn = null;
    switch (variable) {
      case BILLING_GCP_PROJECT:
        dbColumn = GcpBillingTableSchema.projectName;
        break;
      case BILLING_GCP_PRODUCT:
        dbColumn = GcpBillingTableSchema.serviceDescription;
        break;
      case BILLING_GCP_SKU:
        dbColumn = GcpBillingTableSchema.skuId;
        break;
      case BILLING_GCP_BILLING_ACCOUNT_ID:
        dbColumn = GcpBillingTableSchema.billingAccountId;
        break;
      case BILLING_AWS_REGION:
        dbColumn = PreAggregatedTableSchema.region;
        break;
      case BILLING_AWS_SERVICE:
        dbColumn = PreAggregatedTableSchema.serviceCode;
        break;
      case BILLING_AWS_USAGE_TYPE:
        dbColumn = PreAggregatedTableSchema.usageType;
        break;
      case BILLING_AWS_INSTANCE_TYPE:
        dbColumn = PreAggregatedTableSchema.instanceType;
        break;
      case BILLING_AWS_LINKED_ACCOUNT:
        dbColumn = PreAggregatedTableSchema.usageAccountId;
        break;
      default:
        return null;
    }

    Condition condition;
    switch (operator) {
      case EQUALS:
        condition = BinaryCondition.equalTo(dbColumn, values[0]);
        break;
      case IN:
        return new InCondition(dbColumn, getValues());
      case NOT_IN:
        condition = BinaryCondition.notEqualTo(dbColumn, values);
        break;
      case NOT_NULL:
        condition = UnaryCondition.isNotNull(dbColumn);
        break;
      default:
        return null;
    }

    return condition;
  }
}
