/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.alert.cv;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.alert.AlertData;

import software.wings.alerts.AlertStatus;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ContinuousVerificationAlertDataKeys")
@Slf4j
public class ContinuousVerificationAlertData implements AlertData {
  public static final String DEFAULT_TIME_FORMAT = "MMM dd' 'hh:mm a z";

  private CVConfiguration cvConfiguration;
  private MLAnalysisType mlAnalysisType;
  private AlertStatus alertStatus;
  private String logAnomaly;
  private String tag;
  private Set<String> hosts;
  private String portalUrl;
  private String accountId;
  private List<AlertRiskDetail> highRiskTxns;

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
            // if its been less than 4 hours since alert opened, don't open another alert
            if (analysisEndTime - other.analysisEndTime < TimeUnit.HOURS.toMillis(4)) {
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
        if (isNotEmpty(highRiskTxns)) {
          sb.append("\nBelow are the top concerning metrics");
          highRiskTxns.forEach(highRiskTxn
              -> sb.append("\nMetric: ")
                     .append(highRiskTxn.getMetricName())
                     .append(" Group: ")
                     .append(highRiskTxn.getTxnName()));
        }
        break;
      case LOG_ML:
        LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;
        if (logsCVConfiguration.is247LogsV2()) {
          sb.append("\nTag: ").append(tag);
        } else {
          sb.append("\nHosts: ").append(hosts);
        }
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

  @Data
  @Builder
  public static class AlertRiskDetail {
    private String metricName;
    private String txnName;
  }
}
