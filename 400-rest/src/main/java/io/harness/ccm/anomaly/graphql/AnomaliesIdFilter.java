package io.harness.ccm.anomaly.graphql;

import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingIdFilter;
import io.harness.ccm.billing.preaggregated.PreAggregateConstants;

import software.wings.graphql.datafetcher.anomaly.AnomaliesDataTableSchema;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder
@Slf4j
public class AnomaliesIdFilter implements AnomaliesFilter {
  private QLIdOperator operator;
  private AnomaliesDataTableSchema.fields variable;
  private String[] values;
  private static final String[] listOfNullTranslatedFilters = {PreAggregateConstants.entityConstantNoRegion,
      PreAggregateConstants.entityConstantAwsNoLinkedAccount, PreAggregateConstants.entityConstantAwsNoUsageType,
      PreAggregateConstants.entityConstantAwsNoInstanceType, PreAggregateConstants.entityConstantAwsNoService,
      PreAggregateConstants.entityConstantGcpNoProjectId, PreAggregateConstants.entityConstantGcpNoProduct,
      PreAggregateConstants.entityConstantGcpNoSkuId, PreAggregateConstants.entityConstantGcpNoSku,
      PreAggregateConstants.entityNoCloudProviderConst};

  @Override
  public Condition toCondition() {
    Preconditions.checkNotNull(values, "The Anomalies Id filter is missing values");
    Preconditions.checkNotNull(operator, "The Anomalies Id filter is missing operator");

    DbColumn dbColumn = null;
    switch (variable) {
      case CLUSTER_ID:
        dbColumn = AnomaliesDataTableSchema.clusterId;
        break;
      case CLUSTER_NAME:
        dbColumn = AnomaliesDataTableSchema.clusterName;
        break;
      case NAMESPACE:
        dbColumn = AnomaliesDataTableSchema.namespace;
        break;
      case WORKLOAD_TYPE:
        dbColumn = AnomaliesDataTableSchema.workloadType;
        break;
      case GCP_PRODUCT:
        dbColumn = AnomaliesDataTableSchema.gcpProduct;
        break;
      case GCP_PROJECT:
        dbColumn = AnomaliesDataTableSchema.gcpProject;
        break;
      case GCP_SKU_ID:
        dbColumn = AnomaliesDataTableSchema.gcpSkuId;
        break;
      case AWS_ACCOUNT:
        dbColumn = AnomaliesDataTableSchema.awsAccount;
        break;
      case AWS_SERVICE:
        dbColumn = AnomaliesDataTableSchema.awsService;
        break;
      default:
        return null;
    }

    return createCondition(dbColumn);
  }
  private boolean containsNullFilter(List<String> values) {
    for (String filter : listOfNullTranslatedFilters) {
      if (values.contains(filter)) {
        return true;
      }
    }
    return false;
  }
  private Condition createCondition(DbColumn dbColumn) {
    Condition condition;
    boolean containsNullStringConst = containsNullFilter(Arrays.asList(values));
    switch (operator) {
      case EQUALS:
        if (containsNullStringConst) {
          return UnaryCondition.isNull(dbColumn);
        }
        condition = BinaryCondition.equalTo(dbColumn, values[0]);
        break;
      case IN:
        if (containsNullStringConst) {
          return ComboCondition.or(new InCondition(dbColumn, getValues()), UnaryCondition.isNull(dbColumn));
        }
        return new InCondition(dbColumn, getValues());
      case NOT_IN:
        if (containsNullStringConst) {
          return ComboCondition.or(
              new InCondition(dbColumn, getValues()).setNegate(true), UnaryCondition.isNull(dbColumn));
        }
        condition = new InCondition(dbColumn, getValues()).setNegate(true);
        break;
      case NOT_NULL:
        condition = UnaryCondition.isNotNull(dbColumn);
        break;
      case LIKE:
        condition = BinaryCondition.like(dbColumn, "%" + values[0] + "%");
        break;
      default:
        return null;
    }
    return condition;
  }

  public static AnomaliesIdFilter convertFromCloudBillingIdFilter(CloudBillingIdFilter filter) {
    AnomaliesIdFilterBuilder filterBuilder = AnomaliesIdFilter.builder();

    filterBuilder.operator(filter.getOperator());
    filterBuilder.values((String[]) filter.getValues());

    switch (filter.getVariable()) {
      case CloudBillingFilter.BILLING_GCP_PRODUCT:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.GCP_PRODUCT);
        break;
      case CloudBillingFilter.BILLING_GCP_PROJECT:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.GCP_PROJECT);
        break;
      case CloudBillingFilter.BILLING_GCP_SKU:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.GCP_SKU_ID);
        break;
      case CloudBillingFilter.BILLING_AWS_LINKED_ACCOUNT:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.AWS_ACCOUNT);
        break;
      case CloudBillingFilter.BILLING_AWS_SERVICE:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.AWS_SERVICE);
        break;
      default:
        log.error("CloudBillingFilter : {} not supported in AnomaliesIdFilter", filter.getVariable());
    }
    return filterBuilder.build();
  }
}
