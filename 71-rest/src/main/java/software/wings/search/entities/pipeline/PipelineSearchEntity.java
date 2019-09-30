package software.wings.search.entities.pipeline;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

import java.util.List;

@Slf4j
public class PipelineSearchEntity implements SearchEntity<Pipeline> {
  @Inject private PipelineChangeHandler pipelineChangeHandler;
  @Inject private PipelineViewBuilder pipelineViewBuilder;

  public static final String TYPE = "pipelines";
  public static final String VERSION = "0.1";
  public static final Class<Pipeline> SOURCE_ENTITY_CLASS = Pipeline.class;
  private static final String CONFIGURATION_PATH = "pipeline/PipelineSchema.json";
  private static final List<Class<? extends PersistentEntity>> SUBSCRIPTION_ENTITIES =
      ImmutableList.<Class<? extends PersistentEntity>>builder()
          .add(Application.class)
          .add(Service.class)
          .add(Workflow.class)
          .add(Pipeline.class)
          .add(WorkflowExecution.class)
          .add(AuditHeader.class)
          .build();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Class<Pipeline> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public List<Class<? extends PersistentEntity>> getSubscriptionEntities() {
    return SUBSCRIPTION_ENTITIES;
  }

  @Override
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return pipelineChangeHandler;
  }

  @Override
  public PipelineView getView(Pipeline pipeline) {
    return pipelineViewBuilder.createPipelineView(pipeline);
  }
}
