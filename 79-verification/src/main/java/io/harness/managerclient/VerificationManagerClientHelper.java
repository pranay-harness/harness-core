package io.harness.managerclient;

import com.google.inject.Inject;

import io.harness.network.SafeHttpCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.LogAnalysisResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VerificationManagerClientHelper {
  private static final Logger logger = LoggerFactory.getLogger(VerificationManagerClientHelper.class);
  private static final int MAX_RETRY = 3;
  @Inject private VerificationManagerClient managerClient;

  public Map<String, Object> getManagerHeader(String accountId, String analysisVersion) {
    try {
      List<String> versions = SafeHttpCall.execute(managerClient.getListOfPublishedVersions(accountId)).getResource();

      Map<String, Object> headers = new HashMap<>();
      if (versions != null) {
        logger.info("List of available versions is : {} and the analysisVersion is {}", versions, analysisVersion);
        if (analysisVersion != null && versions.contains(analysisVersion)) {
          logger.info("Setting the version info in the manager call to {}", analysisVersion);
          headers.put("Version", analysisVersion);
        }
      }
      return headers;
    } catch (Exception ex) {
      throw new RuntimeException("Error while fetching manager header information");
    }
  }

  public void notifyManagerForLogAnalysis(AnalysisContext context, LogAnalysisResponse response) {
    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try {
        Map<String, Object> headers = getManagerHeader(context.getAccountId(), context.getManagerVersion());
        SafeHttpCall.execute(managerClient.sendNotifyForLogState(headers, context.getCorrelationId(), response));
        break;
      } catch (Exception e) {
        retryCount++;
        logger.error("Exception while sending notify to manager for stateExecutionId {}. correlationId {}",
            context.getStateExecutionId(), context.getCorrelationId());
      }
    }
  }

  public void notifyManagerForMetricAnalysis(AnalysisContext context, MetricDataAnalysisResponse response) {
    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try {
        Map<String, Object> headers = getManagerHeader(context.getAccountId(), context.getManagerVersion());
        SafeHttpCall.execute(managerClient.sendNotifyForMetricState(headers, context.getCorrelationId(), response));
        break;
      } catch (Exception ex) {
        retryCount++;
        logger.error("Error while notifying manager for stateExecutionId {}, correlationId {}",
            context.getStateExecutionId(), context.getCorrelationId(), ex);
      }
    }
  }
}
