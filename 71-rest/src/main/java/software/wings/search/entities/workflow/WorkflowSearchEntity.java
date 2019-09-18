package software.wings.search.entities.workflow;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Workflow;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

@Slf4j
public class WorkflowSearchEntity implements SearchEntity<Workflow> {
  @Inject private WorkflowViewBuilder workflowViewBuilder;
  @Inject private WorkflowChangeHandler workflowChangeHandler;

  public static final String TYPE = "workflows";
  public static final String VERSION = "0.1";
  public static final Class<Workflow> SOURCE_ENTITY_CLASS = Workflow.class;
  private static final String CONFIGURATION_PATH = "workflow/WorkflowSchema.json";

  public String getType() {
    return TYPE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Class<Workflow> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return workflowChangeHandler;
  }

  @Override
  public WorkflowView getView(Workflow workflow) {
    return workflowViewBuilder.createWorkflowView(workflow);
  }
}
