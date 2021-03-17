package software.wings.graphql.datafetcher.workflow.batch;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.persistence.HIterator;

import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.workflow.WorkflowController;
import software.wings.graphql.schema.type.QLWorkflow;
import software.wings.graphql.schema.type.QLWorkflow.QLWorkflowBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.apache.commons.collections4.CollectionUtils;
import org.dataloader.MappedBatchLoader;
import org.mongodb.morphia.query.Query;

@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(Module._380_CG_GRAPHQL)
public class WorkflowBatchDataLoader implements MappedBatchLoader<String, QLWorkflow> {
  @Inject private WingsPersistence wingsPersistence;

  @Inject
  public WorkflowBatchDataLoader(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  public CompletionStage<Map<String, QLWorkflow>> load(Set<String> workflowIds) {
    return CompletableFuture.supplyAsync(() -> {
      Map<String, QLWorkflow> workflowMap = null;
      if (!CollectionUtils.isEmpty(workflowIds)) {
        workflowMap = getWorkflowMap(workflowIds);
      } else {
        workflowMap = Collections.EMPTY_MAP;
      }
      return workflowMap;
    });
  }

  public Map<String, QLWorkflow> getWorkflowMap(@NotNull Set<String> workflowIds) {
    Query<Workflow> query =
        wingsPersistence.createQuery(Workflow.class, excludeAuthority).field(WorkflowKeys.uuid).in(workflowIds);
    Map<String, QLWorkflow> workflowMap = new HashMap<>();

    try (HIterator<Workflow> workflows = new HIterator<>(query.fetch())) {
      workflows.forEach(workflow -> {
        final QLWorkflowBuilder builder = QLWorkflow.builder();
        WorkflowController.populateWorkflow(workflow, builder);
        workflowMap.put(workflow.getUuid(), builder.build());
      });
    }
    return workflowMap;
  }
}
