package software.wings.integration.migration;

import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
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
        //        System.out.println("\nservice: " + service.getUuid());
        wingsPersistence.delete(service);
        deleted++;
      }
    }
    System.out.println("Complete. Deleted " + deleted + " services.");

    List<Workflow> workflows =
        workflowService.listWorkflows(aPageRequest().withLimit(PageRequest.UNLIMITED).build()).getResponse();

    deleted = 0;
    for (Workflow workflow : workflows) {
      boolean missingApp = !appService.exist(workflow.getAppId());

      if (missingApp) {
        //        System.out.println("\nworkflow: " + workflow.getUuid());
        wingsPersistence.delete(workflow);
        deleted++;
      }
    }
    System.out.println("Complete. Deleted " + deleted + " workflows.");

    List<Pipeline> pipelines =
        pipelineService.listPipelines(aPageRequest().withLimit(PageRequest.UNLIMITED).build()).getResponse();

    deleted = 0;
    for (Pipeline pipeline : pipelines) {
      boolean missingApp = !appService.exist(pipeline.getAppId());

      if (missingApp) {
        //        System.out.println("\npipeline: " + pipeline.getUuid());
        wingsPersistence.delete(pipeline);
        deleted++;
      }
    }
    System.out.println("Complete. Deleted " + deleted + " pipelines.");
  }
}
