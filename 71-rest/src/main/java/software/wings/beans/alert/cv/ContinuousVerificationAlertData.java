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
  private static final String DEFAULT_TIME_FORMAT = "MMM dd' 'hh:mm a z";
  private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DEFAULT_TIME_FORMAT);

  private CVConfiguration cvConfiguration;
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
        + ") Time: " + new Date(analysisEndTime) + "\nRisk Score: " + riskScore
        + ", Alert Threshold: " + cvConfiguration.getAlertThreshold();
  }
}
