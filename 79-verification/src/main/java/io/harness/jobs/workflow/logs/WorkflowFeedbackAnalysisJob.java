package io.harness.jobs.workflow.logs;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.jobs.LogMLAnalysisGenerator;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord.LogMLAnalysisRecordKeys;
import software.wings.service.impl.analysis.LogMLAnalysisStatus;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.intfc.DataStoreService;

import java.util.List;
import java.util.concurrent.Callable;

@DisallowConcurrentExecution
@Slf4j
public class WorkflowFeedbackAnalysisJob implements Job {
  @Transient @Inject private LogAnalysisService analysisService;

  @Transient @Inject private LearningEngineService learningEngineService;

  @Transient @Inject private VerificationManagerClientHelper managerClientHelper;
  @Transient @Inject private VerificationManagerClient managerClient;

  @Inject private DataStoreService dataStoreService;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    try {
      String params = jobExecutionContext.getMergedJobDataMap().getString("jobParams");
      AnalysisContext context = JsonUtils.asObject(params, AnalysisContext.class);
      logger.info("Starting feedback analysis cron " + JsonUtils.asJson(context));
      new WorkflowFeedbackAnalysisJob
          .FeedbackAnalysisTask(analysisService, context, jobExecutionContext, learningEngineService, managerClient,
              managerClientHelper, dataStoreService)
          .call();
      logger.info("Finish feedback analysis cron " + context.getStateExecutionId());
    } catch (Exception ex) {
      logger.warn("feedback analysis cron failed with error", ex);
      try {
        jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
      } catch (SchedulerException e) {
        logger.error("Unable to clean up cron", e);
      }
    }
  }

  @AllArgsConstructor
  public static class FeedbackAnalysisTask implements Callable<Long> {
    private LogAnalysisService analysisService;

    private AnalysisContext context;
    private JobExecutionContext jobExecutionContext;
    private LearningEngineService learningEngineService;
    private VerificationManagerClient managerClient;
    private VerificationManagerClientHelper managerClientHelper;
    private DataStoreService dataStoreService;

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

        logAnalysisMinute = analysisService.getLastWorkflowAnalysisMinute(
            context.getAppId(), context.getStateExecutionId(), LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);

        if (logAnalysisMinute < context.getTimeDuration() - 1) {
          // we still need to create new tasks.
          long lastLogMLTaskMinute = analysisService.getLastWorkflowAnalysisMinute(
              context.getAppId(), context.getStateExecutionId(), LogMLAnalysisStatus.LE_ANALYSIS_COMPLETE);
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
              analysisService.updateAnalysisStatus(LogMLAnalysisRecordKeys.stateExecutionId,
                  context.getStateExecutionId(), lastLogMLTaskMinute, LogMLAnalysisStatus.FEEDBACK_ANALYSIS_COMPLETE);
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

      } catch (Exception ex) {
        error = true;
        logger.error(
            "Exception encountered while running feedback analysis task for {}", context.getStateExecutionId());
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
              try {
                jobExecutionContext.getScheduler().deleteJob(jobExecutionContext.getJobDetail().getKey());
              } catch (Exception e) {
                logger.error("Delete cron failed", e);
              }
            }
          }
        } catch (Exception ex) {
          logger.error("analysis failed", ex);
        }
      }

      return -1L;
    }
  }
}
