package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.baseline.WorkflowExecutionBaseline.WORKFLOW_ID_KEY;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.WorkflowType;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Workflow;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.validation.constraints.NotNull;

@Singleton
public interface PipelineViolationCheckerMixin {
  default List<Usage> getPipelineViolationUsages(
      @NotNull List<Pipeline> pipelineList, Predicate<PipelineStageElement> pipelinePredicate) {
    List<Usage> violationUsages = Lists.newArrayList();

    pipelineList.stream().filter(p -> isNotEmpty(p.getPipelineStages())).forEach(p -> {
      boolean hasUsageViolation =
          p.getPipelineStages()
              .stream()
              .filter(ps -> isNotEmpty(ps.getPipelineStageElements()))
              .anyMatch(ps -> ps.getPipelineStageElements().stream().anyMatch(pse -> pipelinePredicate.test(pse)));

      if (hasUsageViolation) {
        violationUsages.add(Usage.builder()
                                .entityId(p.getUuid())
                                .entityName(p.getName())
                                .entityType(EntityType.PIPELINE.name())
                                .property(Pipeline.APP_ID_KEY, p.getAppId())
                                .build());
      }
    });

    return violationUsages;
  }

  default PageRequest<Pipeline> getPipelinesPageRequest(@NotNull String accountId) {
    return (PageRequest<Pipeline>) aPageRequest()
        .withLimit(PageRequest.UNLIMITED)
        .addFilter(Pipeline.ACCOUNT_ID_KEY, EQ, accountId)
        .build();
  }

  default PageRequest<Workflow> getPipelinesPageRequest(@NotNull Pipeline pipeline) {
    PageRequest<Workflow> workflowPageRequest = null;
    Set<String> workflowIds = getWorkflowIdsOfPipeline(pipeline);
    if (isNotEmpty(workflowIds)) {
      workflowPageRequest = aPageRequest()
                                .addFilter(Workflow.APP_ID_KEY, EQ, pipeline.getAppId())
                                .addFilter("workflowType", Operator.EQ, WorkflowType.ORCHESTRATION)
                                .addFilter(Workflow.ID_KEY, IN, workflowIds.toArray())
                                .build();
    }
    return workflowPageRequest;
  }

  default Set<String> getWorkflowIdsOfPipeline(@NotNull Pipeline pipeline) {
    Set<String> workflowIds = Sets.newHashSet();
    if (isNotEmpty(pipeline.getPipelineStages())) {
      pipeline.getPipelineStages()
          .stream()
          .filter(ps -> isNotEmpty(ps.getPipelineStageElements()))
          .map(ps -> ps.getPipelineStageElements())
          .filter(pses -> isNotEmpty(pses))
          .forEach(pses -> {
            pses.forEach(pse -> {
              if (pse != null && isNotEmpty(pse.getProperties()) && pse.getProperties().containsKey(WORKFLOW_ID_KEY)) {
                workflowIds.add((String) pse.getProperties().get(WORKFLOW_ID_KEY));
              }
            });
          });
    }

    return workflowIds;
  }
}
