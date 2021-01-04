package io.harness.ccm.anomaly.graphql;

import io.harness.ccm.anomaly.graphql.AnomaliesIdFilter.AnomaliesIdFilterBuilder;
import io.harness.ccm.anomaly.graphql.AnomaliesTimeFilter.AnomaliesTimeFilterBuilder;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingIdFilter;
import io.harness.ccm.billing.graphql.CloudBillingTimeFilter;

import software.wings.graphql.datafetcher.anomaly.AnomaliesDataTableSchema;
import software.wings.graphql.schema.type.aggregation.Filter;

import com.healthmarketscience.sqlbuilder.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface AnomaliesFilter extends Filter {
  Logger log = LoggerFactory.getLogger(AnomaliesFilter.class);

  Condition toCondition();

  static AnomaliesFilter convertFromCloudBillingFilter(CloudBillingFilter filter) {
    if (filter.getRegion() != null) {
      return convertFromCloudBillingFilter(filter.getRegion());
    }

    if (filter.getCloudProvider() != null) {
      return convertFromCloudBillingFilter(filter.getCloudProvider());
    }

    if (filter.getBillingAccountId() != null) {
      return convertFromCloudBillingFilter(filter.getBillingAccountId());
    }

    // --- gcp ---

    if (filter.getProject() != null) {
      return convertFromCloudBillingFilter(filter.getProject());
    }
    if (filter.getProjectId() != null) {
      return convertFromCloudBillingFilter(filter.getProjectId());
    }
    if (filter.getSku() != null) {
      return convertFromCloudBillingFilter(filter.getSku());
    }
    if (filter.getLinkedAccount() != null) {
      return convertFromCloudBillingFilter(filter.getLinkedAccount());
    }

    // ---- aws ---

    if (filter.getAwsLinkedAccount() != null) {
      return convertFromCloudBillingFilter(filter.getAwsLinkedAccount());
    }
    if (filter.getAwsService() != null) {
      return convertFromCloudBillingFilter(filter.getAwsService());
    }
    if (filter.getAwsUsageType() != null) {
      return convertFromCloudBillingFilter(filter.getAwsUsageType());
    }
    if (filter.getAwsInstanceType() != null) {
      return convertFromCloudBillingFilter(filter.getAwsInstanceType());
    }

    // ---- time filters ---

    if (filter.getPreAggregatedStartTime() != null) {
      return convertFromCloudBillingFilter(filter.getPreAggregatedStartTime());
    }
    if (filter.getPreAggregatedEndTime() != null) {
      return convertFromCloudBillingFilter(filter.getPreAggregatedEndTime());
    }

    if (filter.getPreAggregatedStartTime() != null) {
      return convertFromCloudBillingFilter(filter.getPreAggregatedStartTime());
    }
    if (filter.getPreAggregatedEndTime() != null) {
      return convertFromCloudBillingFilter(filter.getPreAggregatedEndTime());
    }
    return null;
  }

  static AnomaliesFilter convertFromCloudBillingFilter(CloudBillingIdFilter filter) {
    AnomaliesIdFilterBuilder filterBuilder =
        AnomaliesIdFilter.builder().operator(filter.getOperator()).values(filter.getValues());

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

  static AnomaliesFilter convertFromCloudBillingFilter(CloudBillingTimeFilter filter) {
    AnomaliesTimeFilterBuilder filterBuilder = AnomaliesTimeFilter.builder();
    filterBuilder.operator(filter.getOperator());
    filterBuilder.value(filter.getValue());
    switch (filter.getVariable()) {
      case CloudBillingFilter.BILLING_GCP_STARTTIME:
      case CloudBillingFilter.BILLING_GCP_ENDTIME:
      case CloudBillingFilter.BILLING_AWS_STARTTIME:
        filterBuilder.variable(AnomaliesDataTableSchema.fields.ANOMALY_TIME);
        break;
      default:
        log.error("CloudBillingFilter : {} not supported in AnomaliesIdFilter", filter.getVariable());
    }
    return filterBuilder.build();
  }
}
