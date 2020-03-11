package io.harness.ccm.billing;

import com.healthmarketscience.sqlbuilder.Condition;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class GcpBillingFilter {
  public static final String BILLING_GCP_STARTTIME = "billing/gcp/starttime";
  public static final String BILLING_GCP_ENDTIME = "billing/gcp/endtime";

  BillingTimeFilter startTime;
  BillingTimeFilter endTime;

  public BillingTimeFilter getStartTime() {
    startTime.setVariable(BILLING_GCP_STARTTIME);
    return startTime;
  }

  public BillingTimeFilter getEndTime() {
    endTime.setVariable(BILLING_GCP_ENDTIME);
    return endTime;
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
    return null;
  }
}
