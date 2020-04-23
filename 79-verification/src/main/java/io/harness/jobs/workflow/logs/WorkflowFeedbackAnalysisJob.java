package io.harness.jobs.workflow.logs;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.jobs.LogMLAnalysisGenerator;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import software.wings.beans.FeatureName;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.verification.CVActivityLogService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@DisallowConcurrentExecution
@Slf4j
public class WorkflowFeedbackAnalysisJob implements MongoPersistenceIterator.Handler<AnalysisContext> {
  @Transient @Inject private LogAnalysisService analysisService;

  @Transient @Inject private LearningEngineService learningEngineService;

  @Transient @Inject private VerificationManagerClientHelper managerClientHelper;
  @Transient @Inject private VerificationManagerClient managerClient;
  @Inject private CVActivityLogService cvActivityLogService;

  @Inject private DataStoreService dataStoreService;

  @Override
  public void handle(AnalysisContext analysisContext) {
    if (managerClientHelper
            .callManagerWithRetry(
                managerClient.isFeatureEnabled(FeatureName.DISABLE_LOGML_NEURAL_NET, analysisContext.getAccountId()))
            .getResource()) {
      logger.info("The feature  DISABLE_LOGML_NEURAL_NET is enabled for the account {}."
              + " Will not execute handle() in WorkflowFeedbackAnalysisJob",
          analysisContext.getAccountId());
      return;
    }

    logger.info(
        "Handling the feedback for stateExecutionId {} using the iterators", analysisContext.getStateExecutionId());
    try {
      new FeedbackAnalysisTask(analysisService, analysisContext, Optional.empty(), learningEngineService, managerClient,
          managerClientHelper, dataStoreService, cvActivityLogService)
          .call();
    } catch (Exception e) {
      logger.error("Feedback analysis iterator failed for {}", analysisContext.getStateExecutionId(), e);
    }
  }

  @AllArgsConstructor
  public static class FeedbackAnalysisTask implements Callable<Long> {
    private LogAnalysisService analysisService;

    private AnalysisContext context;
    private Optional<JobExecutionContext> jobExecutionContext;
    private LearningEngineService learningEngineService;
    private VerificationManagerClient managerClient;
    private VerificationManagerClientHelper managerClientHelper;
    private DataStoreService dataStoreService;
    private CVActivityLogService cvActivityLogService;

    @Override
    public Long call() throws Exception {
      boolean completeCron = false;
      boolean error = false;
      String errorMsg = "";
      long logAnalysisMinute = -1;
      try {
        logger.info("Running feedback analysis for " + context.getStateExecutionId());
        if (!learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
          logger.warn("Feedback analysis : state is not valid. Stopping cron." + context.getStateExecutionId());
          return -1L;
        }

        if (managerClientHelper.isFeatureFlagEnabled(FeatureName.OUTAGE_CV_DISABLE, context.getAccountId())) {
          cvActivityLogService.getLoggerByStateExecutionId(context.getStateExecutionId())
              .info("Continuous Verification is disabled for your account. Please contact harness support.");
          completeCron = true;
        }

        logAnalysisMinute = analysisService.getLastWorkflowAnalysisMinute(
            context.getAppId(), context.getStateExecutionId(), LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);

        long lastLogMLTaskMinute = analysisService.getLastWorkflowAnalysisMinute(
            context.getAppId(), context.getStateExecutionId(), LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);

        if (!shouldFinishExecution(context, logAnalysisMinute, lastLogMLTaskMinute)) {
          // we still need to create new tasks.

          if (lastLogMLTaskMinute == logAnalysisMinute) {
            logger.info("We are upto date Log feedback tasks for {}. Moving on", context.getStateExecutionId());
          } else {
            // create task for logAnalysisMinute + 1.
            List<CVFeedbackRecord> feedbackRecordList =
                managerClientHelper
                    .callManagerWithRetry(
                        managerClient.getFeedbackList(context.getPredictiveCvConfigId(), context.getStateExecutionId()))
                    .getResource();
            if (EmptyPredicate.isEmpty(feedbackRecordList)) {
              // mark the analysis as feedback complete and move on.
              analysisService.createAndUpdateFeedbackAnalysis(
                  LogMLAnalysisRecordKeys.stateExecutionId, context.getStateExecutionId(), lastLogMLTaskMinute);
              logger.info(
                  "There are no feedbacks available for account {}, stateExecutionId {}. Bumping analysis to feedback complete for minute {}",
                  context.getAccountId(), context.getStateExecutionId(), logAnalysisMinute);
            } else {
              // we have feedbacks, create the task.
              new LogMLAnalysisGenerator(context, lastLogMLTaskMinute, false, analysisService, learningEngineService,
                  managerClient, managerClientHelper, MLAnalysisType.FEEDBACK_ANALYSIS)
                  .run();
            }
          }
        } else {
          completeCron = true;
          logger.info("Log feedback analysis is compelete for all the minutes for state {}. Completing cron",
              context.getStateExecutionId());
        }

      } finally {
        try {
          // send notification to state manager and delete cron.
          if (completeCron || !learningEngineService.isStateValid(context.getAppId(), context.getStateExecutionId())) {
            try {
              logger.info(
                  "send notification to state manager and delete cron with error : {} errorMsg : {}", error, errorMsg);
              new LogMLAnalysisGenerator(context, -1, false, analysisService, learningEngineService, managerClient,
                  managerClientHelper, MLAnalysisType.FEEDBACK_ANALYSIS)
                  .sendStateNotification(context, error, errorMsg, (int) logAnalysisMinute);
            } catch (Exception e) {
              logger.error("Send notification failed for feedback analysis manager", e);
            } finally {
              jobExecutionContext.ifPresent(jobContext -> {
                try {
                  jobContext.getScheduler().deleteJob(jobContext.getJobDetail().getKey());
                } catch (SchedulerException e) {
                  logger.error("Delete cron failed", e);
                }
              });
            }
          }
        } catch (Exception ex) {
          logger.error("analysis failed", ex);
        }
      }

      return -1L;
    }

    private boolean shouldFinishExecution(AnalysisContext context, long currentFeedbackMin, long maxLEAnalysisMin) {
      boolean shouldFinishAnalysis = false;
      if (currentFeedbackMin >= analysisService.getEndTimeForLogAnalysis(context)) {
        shouldFinishAnalysis = true;
      } else {
        if (analysisService.isProcessingComplete(context.getQuery(), context.getAppId(), context.getStateExecutionId(),
                context.getStateType(), context.getTimeDuration(), context.getStartDataCollectionMinute(),
                context.getAccountId())) {
          if (currentFeedbackMin == maxLEAnalysisMin) {
            shouldFinishAnalysis = true;
          }
        }
      }
      return shouldFinishAnalysis;
    }
  }
}
