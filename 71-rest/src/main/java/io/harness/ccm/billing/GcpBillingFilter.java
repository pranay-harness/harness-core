package io.harness.ccm.billing;

import com.healthmarketscience.sqlbuilder.Condition;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class GcpBillingFilter {
  public static final String BILLING_GCP_STARTTIME = "billing/gcp/starttime";
  public static final String BILLING_GCP_ENDTIME = "billing/gcp/endtime";
  public static final String BILLING_GCP_PROJECT = "billing/gcp/project";
  public static final String BILLING_GCP_PRODUCT = "billing/gcp/product";
  public static final String BILLING_GCP_SKU = "billing/gcp/sku";
  public static final String BILLING_GCP_BILLING_ACCOUNT_ID = "billing/gcp/billingaccountid";

  BillingTimeFilter startTime;
  BillingTimeFilter endTime;
  BillingIdFilter project;
  BillingIdFilter product;
  BillingIdFilter sku;
  BillingIdFilter billingAccountId;

  public BillingTimeFilter getStartTime() {
    startTime.setVariable(BILLING_GCP_STARTTIME);
    return startTime;
  }

  public BillingTimeFilter getEndTime() {
    endTime.setVariable(BILLING_GCP_ENDTIME);
    return endTime;
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

  public Condition toCondition() {
    if (startTime != null) {
      BillingTimeFilter startTimeFilter = getStartTime();
      if (null == startTimeFilter.getValue()) {
        logger.error("The GCP billing time filter is missing value.");
      }
      if (null == startTimeFilter.getOperator()) {
        logger.error("The billing time filter is missing operator");
      }
      return startTimeFilter.toCondition();
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
    return null;
  }
}
