package software.wings.beans.alert.cv;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.AlertData;
import software.wings.verification.CVConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
@Builder
public class ContinuousVerificationAlertData implements AlertData {
  private static final Logger log = LoggerFactory.getLogger(ContinuousVerificationAlertData.class);
  public static final String DEFAULT_TIME_FORMAT = "MMM dd' 'hh:mm a z";

  private CVConfiguration cvConfiguration;
  private String portalUrl;
  private String accountId;

  @Default private double riskScore = -1;
  private long analysisStartTime;
  private long analysisEndTime;

  @Override
  public boolean matches(AlertData alertData) {
    ContinuousVerificationAlertData other = (ContinuousVerificationAlertData) alertData;

    return StringUtils.equals(cvConfiguration.getUuid(), other.getCvConfiguration().getUuid())
        && StringUtils.equals(portalUrl, other.getPortalUrl()) && StringUtils.equals(accountId, other.getAccountId())
        && analysisStartTime == other.getAnalysisStartTime() && analysisEndTime == other.getAnalysisEndTime();
  }

  @Override
  public String buildTitle() {
    double alertThreshold =
        BigDecimal.valueOf(cvConfiguration.getAlertThreshold()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    riskScore = BigDecimal.valueOf(riskScore).setScale(2, RoundingMode.HALF_UP).doubleValue();
    return "24/7 Service Guard detected anomalies (Risk Level: High) for " + cvConfiguration.getName()
        + "(Application: " + cvConfiguration.getAppName() + ", Service: " + cvConfiguration.getServiceName()
        + ", Environment: " + cvConfiguration.getEnvName()
        + ") Time: " + new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(new Date(analysisEndTime))
        + "\nRisk Score: " + riskScore + ", Alert Threshold: " + alertThreshold + "\nCheck at: " + portalUrl
        + "/#/account/" + accountId + "/24-7-service-guard/" + cvConfiguration.getServiceId() + "/details";
  }
}
