package io.harness.ccm.billing.graphql;

import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_AWS_INSTANCE_TYPE;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_AWS_LINKED_ACCOUNT;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_AWS_SERVICE;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_AWS_USAGE_TYPE;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_GCP_BILLING_ACCOUNT_ID;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_GCP_PRODUCT;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_GCP_PROJECT;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_GCP_SKU;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_REGION;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.CLOUD_PROVIDER;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.preaggregated.PreAggregateConstants;
import io.harness.ccm.billing.preaggregated.PreAggregatedTableSchema;
import lombok.Builder;
import lombok.Data;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

import java.util.Arrays;

@Data
@Builder
public class CloudBillingIdFilter implements Filter {
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
      case CLOUD_PROVIDER:
        dbColumn = PreAggregatedTableSchema.cloudProvider;
        break;
      case BILLING_GCP_PROJECT:
        dbColumn = PreAggregatedTableSchema.gcpProjectId;
        break;
      case BILLING_GCP_PRODUCT:
        dbColumn = PreAggregatedTableSchema.gcpProduct;
        break;
      case BILLING_GCP_SKU:
        dbColumn = PreAggregatedTableSchema.gcpSkuDescription;
        break;
      case BILLING_GCP_BILLING_ACCOUNT_ID:
        dbColumn = PreAggregatedTableSchema.gcpBillingAccountId;
        break;
      case BILLING_REGION:
        dbColumn = PreAggregatedTableSchema.region;
        break;
      case BILLING_AWS_SERVICE:
        dbColumn = PreAggregatedTableSchema.awsServiceCode;
        break;
      case BILLING_AWS_USAGE_TYPE:
        dbColumn = PreAggregatedTableSchema.awsUsageType;
        break;
      case BILLING_AWS_INSTANCE_TYPE:
        dbColumn = PreAggregatedTableSchema.awsInstanceType;
        break;
      case BILLING_AWS_LINKED_ACCOUNT:
        dbColumn = PreAggregatedTableSchema.awsUsageAccountId;
        break;
      default:
        return null;
    }

    boolean containsNullStringConst = Arrays.asList(values).contains(PreAggregateConstants.nullStringValueConstant);

    Condition condition;
    switch (operator) {
      case EQUALS:
        if (containsNullStringConst) {
          return UnaryCondition.isNull(dbColumn);
        }
        condition = BinaryCondition.equalTo(dbColumn, values[0]);
        break;
      case IN:
        if (containsNullStringConst) {
          return ComboCondition.and(new InCondition(dbColumn, getValues()), UnaryCondition.isNull(dbColumn));
        }
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
