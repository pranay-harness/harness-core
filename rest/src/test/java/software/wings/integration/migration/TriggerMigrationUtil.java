package software.wings.integration.migration;

import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.trigger.Trigger.Builder.aDeploymentTrigger;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerCondition;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by sgurubelli on 10/27/17.
 */
@Integration
@Ignore
@SetupScheduler
public class TriggerMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private TriggerService triggerService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private PipelineService piplineService;

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void migrateStreamActionsToTriggers() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    List<Application> applications = wingsPersistence.query(Application.class, pageRequest).getResponse();

    if (CollectionUtils.isEmpty(applications)) {
      System.out.println("No applications found ");
      return;
    }

    applications.forEach(app -> {
      System.out.println("Fetching artifact streams for applications  = " + app);
      PageRequest<ArtifactStream> artifactStreamPageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter(aSearchFilter().withField("appId", EQ, app.getUuid()).build())
              .build();
      List<ArtifactStream> artifactStreams = artifactStreamService.list(artifactStreamPageRequest).getResponse();
      if (CollectionUtils.isEmpty(artifactStreams)) {
        System.out.println("No artifact streams found for appId = " + app.getUuid());
        return;
      }
      artifactStreams.forEach(artifactStream -> {
        List<ArtifactStreamAction> streamActions = artifactStream.getStreamActions();
        if (CollectionUtils.isEmpty(streamActions)) {
          System.out.println("No stream actions defined for artifact stream = " + artifactStream);
        }
        int nameIdx = 0;
        for (ArtifactStreamAction artifactStreamAction : streamActions) {
          Service service = serviceResourceService.get(app.getUuid(), artifactStream.getServiceId());
          Trigger trigger =
              aDeploymentTrigger()
                  .withUuid(artifactStreamAction.getUuid())
                  .withName(artifactStream.getSourceName() + " " + service.getName() + String.valueOf(nameIdx++))
                  .withAppId(app.getUuid())
                  .withPipelineId(artifactStreamAction.getWorkflowId())
                  .withWorkflowType(artifactStreamAction.getWorkflowType())
                  .build();
          TriggerCondition triggerCondition;
          if (artifactStreamAction.isCustomAction()) {
            System.out.println("Migrating custom trigger to new ScheduledTrigger = " + artifactStreamAction);
            triggerCondition = ScheduledTriggerCondition.builder()
                                   .cronDescription(artifactStreamAction.getCronDescription())
                                   .cronExpression(artifactStreamAction.getCronExpression())
                                   .build();
          } else if (artifactStreamAction.isWebHook()) {
            System.out.println("Migrating webhook trigger to new WebHookTriggerCondition = " + artifactStreamAction);
            triggerCondition = WebHookTriggerCondition.builder()
                                   .webHookToken(artifactStreamAction.getWebHookToken())
                                   .requestBody(artifactStreamAction.getRequestBody())
                                   .artifactStreamId(artifactStream.getUuid())
                                   .build();
          } else {
            System.out.println(
                "Migrating artifact stream action to new ArtifactTriggerCondition = " + artifactStreamAction);
            triggerCondition = ArtifactTriggerCondition.builder()
                                   .artifactFilter(artifactStreamAction.getArtifactFilter())
                                   .artifactStreamId(artifactStream.getUuid())
                                   .build();
          }
          trigger.setCondition(triggerCondition);
          triggerService.save(trigger);
        }
      });
    });
  }
}
