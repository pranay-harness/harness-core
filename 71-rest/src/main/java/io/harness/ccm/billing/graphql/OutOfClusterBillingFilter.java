package io.harness.ccm.billing.graphql;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.Condition;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@Slf4j
public class OutOfClusterBillingFilter {
  public static final String BILLING_GCP_STARTTIME = "billing/gcp/starttime";
  public static final String BILLING_GCP_ENDTIME = "billing/gcp/endtime";
  public static final String BILLING_AWS_STARTTIME = "billing/gcp/starttime";
  public static final String BILLING_GCP_PROJECT = "billing/gcp/project";
  public static final String BILLING_GCP_PRODUCT = "billing/gcp/product";
  public static final String BILLING_GCP_SKU = "billing/gcp/sku";
  public static final String BILLING_GCP_BILLING_ACCOUNT_ID = "billing/gcp/billingaccountid";
  public static final String BILLING_AWS_REGION = "billing/aws/region";
  public static final String BILLING_AWS_LINKED_ACCOUNT = "billing/aws/linkedAccount";
  public static final String BILLING_AWS_USAGE_TYPE = "billing/aws/usageType";
  public static final String BILLING_AWS_INSTANCE_TYPE = "billing/aws/instanceType";
  public static final String BILLING_AWS_SERVICE = "billing/aws/service";
  public static final String GCP_TIME_FILTER_ERROR = "Invalid GCP billing time filter.";
  public static final String AWS_TIME_FILTER_ERROR = "Invalid GCP billing time filter.";

  BillingTimeFilter startTime;
  BillingTimeFilter endTime;
  BillingTimeFilter awsStartTime;
  BillingIdFilter project;
  BillingIdFilter product;
  BillingIdFilter sku;
  BillingIdFilter billingAccountId;
  BillingIdFilter awsRegion;
  BillingIdFilter linkedAccount;
  BillingIdFilter usageType;
  BillingIdFilter instanceType;
  BillingIdFilter service;
  BillingIdFilter cloudProvider;

  public BillingTimeFilter getStartTime() {
    if (startTime == null) {
      return null;
    }
    Preconditions.checkNotNull(startTime.getValue(), GCP_TIME_FILTER_ERROR);
    Preconditions.checkNotNull(startTime.getOperator(), GCP_TIME_FILTER_ERROR);
    startTime.setVariable(BILLING_GCP_STARTTIME);
    return startTime;
  }

  public BillingTimeFilter getEndTime() {
    if (endTime == null) {
      return null;
    }
    Preconditions.checkNotNull(endTime.getValue(), GCP_TIME_FILTER_ERROR);
    Preconditions.checkNotNull(endTime.getOperator(), GCP_TIME_FILTER_ERROR);
    endTime.setVariable(BILLING_GCP_ENDTIME);
    return endTime;
  }

  public BillingTimeFilter getAwsStartTime() {
    if (awsStartTime == null) {
      return null;
    }
    Preconditions.checkNotNull(awsStartTime.getValue(), AWS_TIME_FILTER_ERROR);
    Preconditions.checkNotNull(awsStartTime.getOperator(), AWS_TIME_FILTER_ERROR);
    awsStartTime.setVariable(BILLING_AWS_STARTTIME);
    return awsStartTime;
  }

  public BillingIdFilter getProject() {
    project.setVariable(BILLING_GCP_PROJECT);
    return project;
  }

  public BillingIdFilter getProduct() {
    product.setVariable(BILLING_GCP_PRODUCT);
    return product;
  }

  public BillingIdFilter getSku() {
    sku.setVariable(BILLING_GCP_SKU);
    return sku;
  }

  public BillingIdFilter getBillingAccountId() {
    billingAccountId.setVariable(BILLING_GCP_BILLING_ACCOUNT_ID);
    return billingAccountId;
  }

  public BillingIdFilter getAwsRegion() {
    awsRegion.setVariable(BILLING_AWS_REGION);
    return awsRegion;
  }

  public BillingIdFilter getService() {
    service.setVariable(BILLING_AWS_SERVICE);
    return service;
  }

  public BillingIdFilter getUsageType() {
    usageType.setVariable(BILLING_AWS_USAGE_TYPE);
    return usageType;
  }

  public BillingIdFilter getInstanceType() {
    instanceType.setVariable(BILLING_AWS_INSTANCE_TYPE);
    return instanceType;
  }

  public BillingIdFilter getLinkedAccount() {
    linkedAccount.setVariable(BILLING_AWS_LINKED_ACCOUNT);
    return linkedAccount;
  }

  public Condition toCondition() {
    if (startTime != null) {
      return getStartTime().toCondition();
    }
    if (endTime != null) {
      return getEndTime().toCondition();
    }
    if (project != null) {
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
    if (awsRegion != null) {
      return getAwsRegion().toCondition();
    }
    if (service != null) {
      return getService().toCondition();
    }
    if (usageType != null) {
      return getUsageType().toCondition();
    }
    if (instanceType != null) {
      return getInstanceType().toCondition();
    }
    if (linkedAccount != null) {
      return getLinkedAccount().toCondition();
    }
    if (awsStartTime != null) {
      return getAwsStartTime().toCondition();
    }
    return null;
  }
}
