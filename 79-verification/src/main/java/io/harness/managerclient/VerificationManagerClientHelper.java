package io.harness.managerclient;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.network.SafeHttpCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
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
      List<String> versions = callManagerWithRetry(managerClient.getListOfPublishedVersions(accountId)).getResource();
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
    Map<String, Object> headers = getManagerHeader(context.getAccountId(), context.getManagerVersion());
    callManagerWithRetry(managerClient.sendNotifyForLogState(headers, context.getCorrelationId(), response));
  }

  public void notifyManagerForMetricAnalysis(AnalysisContext context, MetricDataAnalysisResponse response) {
    Map<String, Object> headers = getManagerHeader(context.getAccountId(), context.getManagerVersion());
    callManagerWithRetry(managerClient.sendNotifyForMetricState(headers, context.getCorrelationId(), response));
  }

  public <T> T callManagerWithRetry(Call<T> call) {
    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try {
        return SafeHttpCall.execute(call);
      } catch (Exception ex) {
        retryCount++;
        logger.error(
            "Error while calling manager for call {}, retryCount: {}", call.request().toString(), retryCount, ex);
      }
    }
    throw new WingsException(
        "Exception occurred while calling manager from verification service. Call: " + call.request().toString());
  }
}
