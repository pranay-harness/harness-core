package software.wings.scheduler;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ExecutionArgs;
import software.wings.service.intfc.WorkflowExecutionService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 10/24/16.
 */
public class WorkflowJob implements Job {
  @Inject private WorkflowExecutionService workflowExecutionService;
  private final Logger logger = LoggerFactory.getLogger(WorkflowJob.class);

  public WorkflowJob() {}

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
    logger.error("Executing workflow");
    workflowExecutionService.triggerOrchestrationExecution("APP_ID", "ENV_ID", "ORC_ID", new ExecutionArgs());
    logger.error("Finished executing workflow");
  }
}
