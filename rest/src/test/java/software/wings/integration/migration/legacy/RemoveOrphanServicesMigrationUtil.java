package software.wings.integration.migration.legacy;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import java.util.List;

/**
 * Migration script to delete orphaned services.
 * @author brett on 10/11/17
 */
@Integration
@Ignore
public class RemoveOrphanServicesMigrationUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(RemoveOrphanServicesMigrationUtil.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;

  @Test
  public void removeOrphanedServices() {
    List<Service> services = wingsPersistence.createQuery(Service.class).asList();
    int deleted = 0;
    for (Service service : services) {
      boolean missingApp = !appService.exist(service.getAppId());

      if (missingApp) {
        wingsPersistence.delete(service);
        deleted++;
      }
    }
    logger.info("Complete. Deleted " + deleted + " services.");

    List<Workflow> workflows =
        workflowService.listWorkflows(aPageRequest().withLimit(PageRequest.UNLIMITED).build()).getResponse();

    deleted = 0;
    for (Workflow workflow : workflows) {
      boolean missingApp = !appService.exist(workflow.getAppId());

      if (missingApp) {
        wingsPersistence.delete(workflow);
        deleted++;
      }
    }
    logger.info("Complete. Deleted " + deleted + " workflows.");

    List<Pipeline> pipelines =
        pipelineService.listPipelines(aPageRequest().withLimit(PageRequest.UNLIMITED).build()).getResponse();

    deleted = 0;
    for (Pipeline pipeline : pipelines) {
      boolean missingApp = !appService.exist(pipeline.getAppId());

      if (missingApp) {
        wingsPersistence.delete(pipeline);
        deleted++;
      }
    }
    logger.info("Complete. Deleted " + deleted + " pipelines.");
  }
}
