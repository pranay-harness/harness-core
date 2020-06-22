package migrations.all;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Account;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.StateType;

import java.util.List;

@Slf4j
@Singleton
public class MigrateServiceNowCriteriaInPipelines implements Migration {
  @Inject private PipelineService pipelineService;
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;
  private final String DEBUG_LINE = " SERVICENOW_CRITERIA_MIGRATION: ";

  @Inject private AccountService accountService;

  public void migrate() {
    List<Account> allAccounts = accountService.listAllAccountWithDefaultsWithoutLicenseInfo();
    for (Account account : allAccounts) {
      String accountId = account.getUuid();
      logger.info(StringUtils.join(DEBUG_LINE, "Starting Servicenow Critreia migration for accountId:", accountId));
      migrate(account);
    }
  }

  public void migrate(Account account) {
    logger.info(StringUtils.join(
        DEBUG_LINE, "Starting Servicenow Critreia migration for Pipelines, accountId ", account.getUuid()));

    List<Pipeline> pipelines = WorkflowAndPipelineMigrationUtils.fetchAllPipelinesForAccount(
        wingsPersistence, pipelineService, account.getUuid());

    for (Pipeline pipeline : pipelines) {
      try {
        migrate(pipelineService.readPipeline(pipeline.getAppId(), pipeline.getUuid(), true));
      } catch (Exception e) {
        logger.error("[SERIVCENOW_CRITERIA_MIGRATION] Migration failed for PipelineId: " + pipeline.getUuid()
            + ExceptionUtils.getMessage(e));
      }
    }
  }

  public void migrate(Pipeline pipeline) {
    boolean modified = false;
    // Migrate each stage

    for (PipelineStage stage : pipeline.getPipelineStages()) {
      PipelineStageElement stageElement = stage.getPipelineStageElements().get(0);

      if (stageElement.getType().equals(StateType.APPROVAL.name())) {
        logger.info("Migrating approval state in pipeline");
        modified =
            WorkflowAndPipelineMigrationUtils.updateServiceNowProperties(stageElement.getProperties()) || modified;
      }
    }

    if (modified) {
      try {
        pipelineService.update(pipeline, true, false);
        logger.info("--- Pipeline updated: {}, {}", pipeline.getUuid(), pipeline.getName());
        Thread.sleep(100);
      } catch (Exception e) {
        logger.error("[SERVICENOW_CRITERIA_ERROR] Error updating pipeline " + pipeline.getUuid(), e);
      }
    }
  }
}
