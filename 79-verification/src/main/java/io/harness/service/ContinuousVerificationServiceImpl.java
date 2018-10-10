package io.harness.service;

import static software.wings.common.VerificationConstants.getMetricAnalysisStates;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;

import com.google.inject.Inject;

import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.service.intfc.ContinuousVerificationService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 10/9/18.
 */
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceImpl.class);

  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Inject private VerificationManagerClientHelper verificationManagerClientHelper;
  @Inject private VerificationManagerClient verificationManagerClient;

  @Override
  public boolean triggerDataCollection(String accountId) {
    List<CVConfiguration> cvConfigurations = cvConfigurationService.listConfigurations(accountId);
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
    cvConfigurations.stream().filter(cvConfiguration -> cvConfiguration.isEnabled24x7()).forEach(cvConfiguration -> {
      long maxCVCollectionMinute =
          timeSeriesAnalysisService.getMaxCVCollectionMinute(cvConfiguration.getAppId(), cvConfiguration.getUuid());
      long startTime = maxCVCollectionMinute <= 0 || endMinute - maxCVCollectionMinute > PREDECTIVE_HISTORY_MINUTES
          ? TimeUnit.MINUTES.toMillis(endMinute) - PREDECTIVE_HISTORY_MINUTES
          : TimeUnit.MINUTES.toMillis(maxCVCollectionMinute + 1);
      long endTime = TimeUnit.MINUTES.toMillis(endMinute);
      if (getMetricAnalysisStates().contains(cvConfiguration.getStateType())) {
        logger.info("triggering data collection for state {} config {} startTime {} endTime {} collectionMinute {}",
            cvConfiguration.getStateType(), cvConfiguration.getUuid(), startTime, endMinute, endMinute);
        verificationManagerClientHelper.callManagerWithRetry(verificationManagerClient.triggerAPMDataCollection(
            cvConfiguration.getUuid(), cvConfiguration.getStateType(), startTime, endTime, (int) endMinute));
      }
    });
    return true;
  }
}
