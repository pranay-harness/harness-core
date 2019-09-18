package software.wings.search.entities.deployment;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.WorkflowExecution;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

@Slf4j
public class DeploymentSearchEntity implements SearchEntity<WorkflowExecution> {
  @Inject private DeploymentViewBuilder deploymentViewBuilder;
  @Inject private DeploymentChangeHandler deploymentChangeHandler;

  public static final String TYPE = "deployments";
  public static final String VERSION = "0.1";
  public static final Class<WorkflowExecution> SOURCE_ENTITY_CLASS = WorkflowExecution.class;
  private static final String CONFIGURATION_PATH = "deployment/DeploymentSchema.json";

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
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return deploymentChangeHandler;
  }

  @Override
  public DeploymentView getView(WorkflowExecution workflowExecution) {
    return deploymentViewBuilder.createDeploymentView(workflowExecution);
  }
}
