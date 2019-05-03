package software.wings.beans.alert.cv;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.AlertData;
import software.wings.service.impl.analysis.MLAnalysisType;
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
  private MLAnalysisType mlAnalysisType;
  private String logAnomaly;
  private String portalUrl;
  private String accountId;

  @Default private double riskScore = -1;
  private long analysisStartTime;
  private long analysisEndTime;

  @Override
  public boolean matches(AlertData alertData) {
    ContinuousVerificationAlertData other = (ContinuousVerificationAlertData) alertData;

    return StringUtils.equals(cvConfiguration.getUuid(), other.getCvConfiguration().getUuid())
        && StringUtils.equals(logAnomaly, other.logAnomaly) && analysisStartTime == other.getAnalysisStartTime()
        && analysisEndTime == other.getAnalysisEndTime();
  }

  @Override
  public String buildTitle() {
    double alertThreshold =
        BigDecimal.valueOf(cvConfiguration.getAlertThreshold()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    riskScore = BigDecimal.valueOf(riskScore).setScale(2, RoundingMode.HALF_UP).doubleValue();
    StringBuilder sb = new StringBuilder()
                           .append("24/7 Service Guard detected anomalies for ")
                           .append(cvConfiguration.getName())
                           .append("(Application: ")
                           .append(cvConfiguration.getAppName())
                           .append(", Service: ")
                           .append(cvConfiguration.getServiceName())
                           .append(", Environment: ")
                           .append(cvConfiguration.getEnvName())
                           .append(") Time: ")
                           .append(new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(new Date(analysisEndTime)));

    switch (mlAnalysisType) {
      case TIME_SERIES:
        sb.append("\nRisk Score: ").append(riskScore).append(", Alert Threshold: ").append(alertThreshold);
        break;
      case LOG_ML:
        sb.append("\nLog Message: ").append(logAnomaly);
        break;
      default:
        throw new IllegalArgumentException("Invalid type: " + mlAnalysisType);
    }

    sb.append("\nCheck at: ")
        .append(portalUrl)
        .append("/#/account/")
        .append(accountId)
        .append("/24-7-service-guard/")
        .append(cvConfiguration.getServiceId())
        .append("/details?cvConfigId")
        .append(cvConfiguration.getUuid())
        .append("&analysisStartTime=")
        .append(analysisStartTime)
        .append("&analysisEndTime=")
        .append(analysisEndTime);
    return sb.toString();
  }
}
