package software.wings.integration.migration;

import static java.util.Arrays.asList;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.trigger.Trigger.Builder.aDeploymentTrigger;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.sm.StateType.ENV_STATE;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
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
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
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
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowService workflowService;

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void migrateStreamActionsToTriggers() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    List<Application> applications = wingsPersistence.query(Application.class, pageRequest).getResponse();
    for (Application app : applications) {
      System.out.println("Fetching artifact streams for applications  = " + app);
      PageRequest<ArtifactStream> artifactStreamPageRequest =
          aPageRequest()
              .withLimit(UNLIMITED)
              .addFilter(aSearchFilter().withField("appId", EQ, app.getUuid()).build())
              .build();
      List<ArtifactStream> artifactStreams = artifactStreamService.list(artifactStreamPageRequest).getResponse();
      if (CollectionUtils.isEmpty(artifactStreams)) {
        System.out.println("No artifact streams found for appId = " + app.getUuid());
        continue;
      }
      int nameIdx = 0;
      for (ArtifactStream artifactStream : artifactStreams) {
        List<ArtifactStreamAction> streamActions = artifactStream.getStreamActions();
        if (CollectionUtils.isEmpty(streamActions)) {
          System.out.println("No stream actions defined for artifact stream = " + artifactStream);
        }
        for (ArtifactStreamAction artifactStreamAction : streamActions) {
          Service service = serviceResourceService.get(app.getUuid(), artifactStream.getServiceId());
          Trigger trigger = triggerService.get(app.getUuid(), artifactStreamAction.getUuid());
          if (trigger == null) {
            trigger = aDeploymentTrigger()
                          .withUuid(artifactStreamAction.getUuid())
                          .withName(String.format(
                              "%s %s%s", artifactStream.getSourceName(), service.getName(), String.valueOf(nameIdx++)))
                          .withAppId(app.getUuid())
                          .withPipelineId(wrapWorkflowWithinPipeline(app.getUuid(), artifactStreamAction))
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
                                     .webHookToken(WebHookToken.builder()
                                                       .webHookToken(artifactStreamAction.getWebHookToken())
                                                       .httpMethod("POST")
                                                       .payload(artifactStreamAction.getRequestBody())
                                                       .build())
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
          } else {
            triggerService.update(trigger);
          }
        }
      }
    }
  }

  @Test
  public void deleteTriggers() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    List<Application> applications = wingsPersistence.query(Application.class, pageRequest).getResponse();
    applications.forEach(app -> triggerService.deleteByApp(app.getUuid()));
  }

  private String wrapWorkflowWithinPipeline(String appId, ArtifactStreamAction artifactStreamAction) {
    if (!artifactStreamAction.getWorkflowType().equals(WorkflowType.ORCHESTRATION)) {
      return artifactStreamAction.getWorkflowId();
    }

    Workflow workflow = workflowService.readWorkflow(appId, artifactStreamAction.getWorkflowId());
    PageRequest<Pipeline> pipelinePageRequest =
        PageRequest.Builder.aPageRequest().addFilter("appId", EQ, appId).build();
    List<Pipeline> pipelines = pipelineService.listPipelines(pipelinePageRequest, false);

    for (Pipeline pipeline : pipelines) {
      if (pipeline.getPipelineStages().size() >= 1) {
        if (pipeline.getPipelineStages()
                .stream()
                .flatMap(pipelineStage -> pipelineStage.getPipelineStageElements().stream())
                .filter(pse
                    -> ENV_STATE.name().equals(pse.getType())
                        && pse.getProperties().get("workflowId").equals(artifactStreamAction.getWorkflowId()))
                .count()
            != 0) {
          return pipeline.getUuid();
        }
      }
    }

    PipelineStage stag1 = new PipelineStage(asList(new PipelineStageElement(workflow.getName(), ENV_STATE.name(),
        ImmutableMap.of("envId", workflow.getEnvId(), "workflowId", workflow.getUuid()))));

    List<PipelineStage> pipelineStages = Collections.singletonList(stag1);

    Pipeline pipeline = aPipeline()
                            .withAppId(workflow.getAppId())
                            .withName(workflow.getName() + " Ported")
                            .withDescription("Ported workflow to pipeline")
                            .withPipelineStages(pipelineStages)
                            .build();
    pipelineService.createPipeline(pipeline);

    return pipeline.getUuid();
  }
}
