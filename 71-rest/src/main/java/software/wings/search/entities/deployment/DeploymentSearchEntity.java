package software.wings.search.entities.deployment;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchEntity;

import java.util.List;

@Slf4j
public class DeploymentSearchEntity implements SearchEntity<WorkflowExecution> {
  @Inject private DeploymentViewBuilder deploymentViewBuilder;
  @Inject private DeploymentChangeHandler deploymentChangeHandler;
  @Inject private DeploymentElasticsearchRequestHandler deploymentElasticsearchRequestHandler;

  public static final String TYPE = "deployments";
  public static final String VERSION = "0.1";
  public static final Class<WorkflowExecution> SOURCE_ENTITY_CLASS = WorkflowExecution.class;
  private static final String CONFIGURATION_PATH = "deployment/DeploymentSchema.json";
  private static final List<Class<? extends PersistentEntity>> SUBSCRIPTION_ENTITIES =
      ImmutableList.<Class<? extends PersistentEntity>>builder()
          .add(Application.class)
          .add(Service.class)
          .add(Workflow.class)
          .add(Environment.class)
          .add(WorkflowExecution.class)
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
  public Class<WorkflowExecution> getSourceEntityClass() {
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
    return deploymentChangeHandler;
  }

  @Override
  public ElasticsearchRequestHandler getElasticsearchRequestHandler() {
    return deploymentElasticsearchRequestHandler;
  }

  @Override
  public DeploymentView getView(WorkflowExecution workflowExecution) {
    return deploymentViewBuilder.createDeploymentView(workflowExecution);
  }
}
