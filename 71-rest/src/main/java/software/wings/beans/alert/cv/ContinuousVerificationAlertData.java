package software.wings.beans.alert.cv;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.AlertData;
import software.wings.verification.CVConfiguration;

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
    return cvConfiguration.getUuid().equals(
        ((ContinuousVerificationAlertData) alertData).getCvConfiguration().getUuid());
  }

  @Override
  public String buildTitle() {
    return "24/7 Service Guard detected anomalies (Risk Level: High) for " + cvConfiguration.getName()
        + "(Application: " + cvConfiguration.getAppName() + ", Environment: " + cvConfiguration.getEnvName()
        + ") Time: " + new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(new Date(analysisEndTime)) + "\nRisk Score: "
        + riskScore + ", Alert Threshold: " + cvConfiguration.getAlertThreshold() + "\nCheck at: " + portalUrl
        + "/#/account/" + accountId + "/24-7-service-guard/" + cvConfiguration.getServiceId() + "/details";
  }
}
