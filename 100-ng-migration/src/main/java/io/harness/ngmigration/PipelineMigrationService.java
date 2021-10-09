package io.harness.ngmigration;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.plancreator.stages.StageElementWrapperConfig;
import io.harness.serializer.JsonUtils;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.CgEntityNode;
import software.wings.ngmigration.DiscoveryNode;
import software.wings.ngmigration.NGMigrationEntity;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGMigrationStatus;
import software.wings.ngmigration.NGYamlFile;
import software.wings.ngmigration.NgMigration;
import software.wings.service.intfc.PipelineService;
import software.wings.sm.StateType;

import com.google.api.client.util.ArrayMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class PipelineMigrationService implements NgMigration {
  @Inject private PipelineService pipelineService;
  @Inject private WorkflowMigrationService workflowMigrationService;
  @Inject private NgMigrationFactory ngMigrationFactory;

  @Override
  public DiscoveryNode discover(NGMigrationEntity entity) {
    Pipeline pipeline = (Pipeline) entity;
    String entityId = pipeline.getUuid();
    CgEntityId pipelineEntityId = CgEntityId.builder().type(NGMigrationEntityType.PIPELINE).id(entityId).build();
    CgEntityNode pipelineNode = CgEntityNode.builder()
                                    .id(entityId)
                                    .type(NGMigrationEntityType.PIPELINE)
                                    .entityId(pipelineEntityId)
                                    .entity(pipeline)
                                    .build();

    Set<CgEntityId> children = new HashSet<>();
    if (EmptyPredicate.isNotEmpty(pipeline.getPipelineStages())) {
      List<PipelineStage> stages = pipeline.getPipelineStages();
      stages.stream().flatMap(stage -> stage.getPipelineStageElements().stream()).forEach(stageElement -> {
        // Handle Approval State
        if (StateType.ENV_STATE.name().equals(stageElement.getType())) {
          String workflowId = (String) stageElement.getProperties().get("workflowId");
          if (EmptyPredicate.isNotEmpty(workflowId)) {
            children.add(CgEntityId.builder().type(NGMigrationEntityType.WORKFLOW).id(workflowId).build());
          }
        }
      });
    }

    return DiscoveryNode.builder().children(children).entityNode(pipelineNode).build();
  }

  @Override
  public DiscoveryNode discover(String accountId, String appId, String entityId) {
    return discover(pipelineService.getPipeline(appId, entityId));
  }

  @Override
  public NGMigrationStatus canMigrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    return null;
  }

  @Override
  public void migrate(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {}

  @Override
  public List<NGYamlFile> getYamls(
      Map<CgEntityId, CgEntityNode> entities, Map<CgEntityId, Set<CgEntityId>> graph, CgEntityId entityId) {
    Pipeline pipeline = (Pipeline) entities.get(entityId).getEntity();

    List<StageElementWrapperConfig> ngStages = new ArrayList<>();
    if (EmptyPredicate.isNotEmpty(pipeline.getPipelineStages())) {
      pipeline.getPipelineStages()
          .stream()
          .flatMap(stage -> stage.getPipelineStageElements().stream())
          .forEach(stageElement -> {
            if (StateType.ENV_STATE.name().equals(stageElement.getType())) {
              String workflowId = (String) stageElement.getProperties().get("workflowId");
              if (EmptyPredicate.isNotEmpty(workflowId)) {
                // Every CG pipeline stage to NG convert
                ngStages.add(workflowMigrationService.getNgStage(
                    entities, graph, CgEntityId.builder().type(NGMigrationEntityType.WORKFLOW).id(workflowId).build()));
              }
            }
            // TODO: Handle Approval State
          });
    }

    List<NGYamlFile> allFiles = new ArrayList<>();

    graph.get(entityId).forEach(entityId1 -> {
      allFiles.addAll(ngMigrationFactory.getMethod(entityId1.getType()).getYamls(entities, graph, entityId1));
    });

    // TODO: @puthraya Fix tags
    PipelineInfoConfig pipelineInfoConfig = PipelineInfoConfig.builder()
                                                .name(pipeline.getName())
                                                .uuid(pipeline.getName())
                                                .projectIdentifier("PROJECT_INPUT_REQUIRED")
                                                .orgIdentifier("ORG_INPUT_REQUIRED")
                                                .stages(ngStages)
                                                .tags(new ArrayMap<>())
                                                .build();

    allFiles.add(
        NGYamlFile.builder().filename(pipeline.getName() + ".yaml").yaml(JsonUtils.asTree(pipelineInfoConfig)).build());

    return allFiles;
  }
}
