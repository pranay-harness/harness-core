package io.harness.ccm.billing.graphql;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.Condition;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Slf4j
public class CloudBillingFilter {
  public static final String BILLING_GCP_STARTTIME = "billing/gcp/starttime";
  public static final String BILLING_GCP_ENDTIME = "billing/gcp/endtime";
  public static final String BILLING_AWS_STARTTIME = "billing/aws/starttime";
  public static final String BILLING_GCP_PROJECT = "billing/gcp/project";
  public static final String BILLING_GCP_PRODUCT = "billing/gcp/product";
  public static final String BILLING_GCP_SKU = "billing/gcp/sku";
  public static final String BILLING_GCP_BILLING_ACCOUNT_ID = "billing/gcp/billingaccountid";
  public static final String BILLING_REGION = "billing/region";
  public static final String BILLING_AWS_LINKED_ACCOUNT = "billing/aws/linkedAccount";
  public static final String BILLING_AWS_USAGE_TYPE = "billing/aws/usageType";
  public static final String BILLING_AWS_INSTANCE_TYPE = "billing/aws/instanceType";
  public static final String BILLING_AWS_SERVICE = "billing/aws/service";
  public static final String GCP_TIME_FILTER_ERROR = "Invalid GCP billing time filter.";
  public static final String AWS_TIME_FILTER_ERROR = "Invalid GCP billing time filter.";
  public static final String CLOUD_PROVIDER = "cloudProvider";

  CloudBillingTimeFilter startTime;
  CloudBillingTimeFilter endTime;
  CloudBillingTimeFilter preAggregatedTableStartTime;
  CloudBillingTimeFilter preAggregatedTableEndTime;
  CloudBillingIdFilter projectId;
  CloudBillingIdFilter product;
  CloudBillingIdFilter sku;
  CloudBillingIdFilter billingAccountId;
  CloudBillingIdFilter region;
  CloudBillingIdFilter awsLinkedAccount;
  CloudBillingIdFilter awsUsageType;
  CloudBillingIdFilter awsInstanceType;
  CloudBillingIdFilter awsService;
  CloudBillingIdFilter cloudProvider;

  public CloudBillingTimeFilter getStartTime() {
    if (startTime == null) {
      return null;
    }
    Preconditions.checkNotNull(startTime.getValue(), GCP_TIME_FILTER_ERROR);
    Preconditions.checkNotNull(startTime.getOperator(), GCP_TIME_FILTER_ERROR);
    startTime.setVariable(BILLING_GCP_STARTTIME);
    return startTime;
  }

  public CloudBillingTimeFilter getEndTime() {
    if (endTime == null) {
      return null;
    }
    Preconditions.checkNotNull(endTime.getValue(), GCP_TIME_FILTER_ERROR);
    Preconditions.checkNotNull(endTime.getOperator(), GCP_TIME_FILTER_ERROR);
    endTime.setVariable(BILLING_GCP_ENDTIME);
    return endTime;
  }

  public CloudBillingTimeFilter getPreAggregatedStartTime() {
    if (preAggregatedTableStartTime == null) {
      return null;
    }
    Preconditions.checkNotNull(preAggregatedTableStartTime.getValue(), AWS_TIME_FILTER_ERROR);
    Preconditions.checkNotNull(preAggregatedTableStartTime.getOperator(), AWS_TIME_FILTER_ERROR);
    preAggregatedTableStartTime.setVariable(BILLING_AWS_STARTTIME);
    return preAggregatedTableStartTime;
  }

  public CloudBillingTimeFilter getPreAggregatedEndTime() {
    if (preAggregatedTableEndTime == null) {
      return null;
    }
    Preconditions.checkNotNull(preAggregatedTableEndTime.getValue(), AWS_TIME_FILTER_ERROR);
    Preconditions.checkNotNull(preAggregatedTableEndTime.getOperator(), AWS_TIME_FILTER_ERROR);
    preAggregatedTableEndTime.setVariable(BILLING_AWS_STARTTIME);
    return preAggregatedTableEndTime;
  }

  public CloudBillingIdFilter getCloudProvider() {
    cloudProvider.setVariable(CLOUD_PROVIDER);
    return cloudProvider;
  }

  public CloudBillingIdFilter getProject() {
    projectId.setVariable(BILLING_GCP_PROJECT);
    return projectId;
  }

  public CloudBillingIdFilter getProduct() {
    product.setVariable(BILLING_GCP_PRODUCT);
    return product;
  }

  public CloudBillingIdFilter getSku() {
    sku.setVariable(BILLING_GCP_SKU);
    return sku;
  }

  public CloudBillingIdFilter getBillingAccountId() {
    billingAccountId.setVariable(BILLING_GCP_BILLING_ACCOUNT_ID);
    return billingAccountId;
  }

  public CloudBillingIdFilter getRegion() {
    region.setVariable(BILLING_REGION);
    return region;
  }

  public CloudBillingIdFilter getService() {
    awsService.setVariable(BILLING_AWS_SERVICE);
    return awsService;
  }

  public CloudBillingIdFilter getUsageType() {
    awsUsageType.setVariable(BILLING_AWS_USAGE_TYPE);
    return awsUsageType;
  }

  public CloudBillingIdFilter getInstanceType() {
    awsInstanceType.setVariable(BILLING_AWS_INSTANCE_TYPE);
    return awsInstanceType;
  }

  public CloudBillingIdFilter getLinkedAccount() {
    awsLinkedAccount.setVariable(BILLING_AWS_LINKED_ACCOUNT);
    return awsLinkedAccount;
  }

  public Condition toCondition() {
    if (startTime != null) {
      return getStartTime().toCondition();
    }
    if (endTime != null) {
      return getEndTime().toCondition();
    }
    if (projectId != null) {
      return getProject().toCondition();
    }
    if (product != null) {
      return getProduct().toCondition();
    }
    if (sku != null) {
      return getSku().toCondition();
    }
    if (billingAccountId != null) {
      return getBillingAccountId().toCondition();
    }
    if (region != null) {
      return getRegion().toCondition();
    }
    if (awsService != null) {
      return getService().toCondition();
    }
    if (awsUsageType != null) {
      return getUsageType().toCondition();
    }
    if (awsInstanceType != null) {
      return getInstanceType().toCondition();
    }
    if (awsLinkedAccount != null) {
      return getLinkedAccount().toCondition();
    }
    if (preAggregatedTableStartTime != null) {
      return getPreAggregatedStartTime().toCondition();
    }
    if (preAggregatedTableEndTime != null) {
      return getPreAggregatedEndTime().toCondition();
    }
    if (cloudProvider != null) {
      return getCloudProvider().toCondition();
    }
    return null;
  }
}
