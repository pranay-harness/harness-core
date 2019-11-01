package software.wings.beans.alert.cv;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.alerts.AlertStatus;
import software.wings.beans.alert.AlertData;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.verification.CVConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class ContinuousVerificationAlertData implements AlertData {
  private static final Logger log = LoggerFactory.getLogger(ContinuousVerificationAlertData.class);
  public static final String DEFAULT_TIME_FORMAT = "MMM dd' 'hh:mm a z";

  private CVConfiguration cvConfiguration;
  private MLAnalysisType mlAnalysisType;
  private AlertStatus alertStatus;
  private String logAnomaly;
  private Set<String> hosts;
  private String portalUrl;
  private String accountId;

  @Default private double riskScore = -1;
  private long analysisStartTime;
  private long analysisEndTime;

  @Override
  public boolean matches(AlertData alertData) {
    ContinuousVerificationAlertData other = (ContinuousVerificationAlertData) alertData;
    if (!StringUtils.equals(cvConfiguration.getUuid(), other.getCvConfiguration().getUuid())) {
      return false;
    }

    switch (mlAnalysisType) {
      case TIME_SERIES:
        switch (alertStatus) {
          case Open:
            // if its been less than an hour since alert opened, don't open another alert
            if (analysisEndTime - other.analysisEndTime < TimeUnit.HOURS.toMillis(1)) {
              return true;
            }
            return false;
          case Closed:
            return true;
          default:
            throw new IllegalArgumentException("invalid type " + mlAnalysisType);
        }
      case LOG_ML:
        return StringUtils.equals(logAnomaly, other.logAnomaly) && analysisStartTime == other.getAnalysisStartTime()
            && analysisEndTime == other.getAnalysisEndTime();
      default:
        throw new IllegalArgumentException("invalid type " + mlAnalysisType);
    }
  }

  @Override
  public String buildTitle() {
    riskScore = BigDecimal.valueOf(riskScore).setScale(2, RoundingMode.HALF_UP).doubleValue();

    StringBuilder sb = new StringBuilder()
                           .append("24/7 Service Guard detected anomalies.\nStatus: Open\nName: ")
                           .append(cvConfiguration.getName())
                           .append("\nApplication: ")
                           .append(cvConfiguration.getAppName())
                           .append("\nService: ")
                           .append(cvConfiguration.getServiceName())
                           .append("\nEnvironment: ")
                           .append(cvConfiguration.getEnvName())
                           .append("\nIncident Time: ")
                           .append(new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(new Date(analysisEndTime)));

    switch (mlAnalysisType) {
      case TIME_SERIES:
        sb.append("\nRisk Score: ").append(riskScore);
        break;
      case LOG_ML:
        sb.append("\nHosts: ").append(hosts);
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
        .append("/details?cvConfigId=")
        .append(cvConfiguration.getUuid())
        .append("&analysisStartTime=")
        .append(analysisStartTime)
        .append("&analysisEndTime=")
        .append(analysisEndTime);
    return sb.toString();
  }

  @Override
  public String buildResolutionTitle() {
    StringBuilder sb = new StringBuilder()
                           .append("Incident raised by 24/7 Service Guard is now resolved.\nStatus: Closed\nName: ")
                           .append(cvConfiguration.getName())
                           .append("\nApplication: ")
                           .append(cvConfiguration.getAppName())
                           .append("\nService: ")
                           .append(cvConfiguration.getServiceName())
                           .append("\nEnvironment: ")
                           .append(cvConfiguration.getEnvName())
                           .append("\nIncident Time: ")
                           .append(new SimpleDateFormat(DEFAULT_TIME_FORMAT).format(new Date(analysisEndTime)));
    return sb.toString();
  }
}
