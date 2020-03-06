package io.harness.managerclient;

import static io.harness.threading.Morpheus.sleep;
import static java.time.Duration.ofMillis;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.exception.WingsException;
import io.harness.network.SafeHttpCall;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.verification.VerificationDataAnalysisResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class VerificationManagerClientHelper {
  private static final int MAX_RETRY = 3;
  @Inject private VerificationManagerClient managerClient;
  @Inject CVActivityLogService cvActivityLogService;

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

  public void notifyManagerForVerificationAnalysis(AnalysisContext context, VerificationDataAnalysisResponse response) {
    Map<String, Object> headers = getManagerHeader(context.getAccountId(), context.getManagerVersion());
    if (response.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      cvActivityLogService.getLoggerByStateExecutionId(context.getStateExecutionId()).info("Analysis successful");
    } else {
      cvActivityLogService.getLoggerByStateExecutionId(context.getStateExecutionId())
          .error("Analysis failed with error: " + response.getStateExecutionData().getErrorMsg());
    }
    callManagerWithRetry(managerClient.sendNotifyForVerificationState(headers, context.getCorrelationId(), response));
  }

  public void notifyManagerForVerificationAnalysis(
      String accountId, String correlationId, VerificationDataAnalysisResponse response) {
    callManagerWithRetry(
        managerClient.sendNotifyForVerificationState(getManagerHeader(accountId, null), correlationId, response));
  }

  public <T> T callManagerWithRetry(Call<T> call) {
    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try {
        return SafeHttpCall.execute(call);
      } catch (Exception ex) {
        retryCount++;
        call = call.clone();
        logger.info(
            "Error while calling manager for call {}, retryCount: {}", call.request().toString(), retryCount, ex);
        sleep(ofMillis(1000));
      }
    }
    logger.error("Error while calling manager for call {} after all retries", call.request().toString());
    throw new WingsException(
        "Exception occurred while calling manager from verification service. Call: " + call.request().toString());
  }
}
