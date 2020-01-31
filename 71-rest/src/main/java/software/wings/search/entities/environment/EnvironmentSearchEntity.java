package software.wings.search.entities.environment;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchEntity;

import java.util.List;

@Slf4j
public class EnvironmentSearchEntity implements SearchEntity<Environment> {
  @Inject private EnvironmentChangeHandler environmentChangeHandler;
  @Inject private EnvironmentViewBuilder environmentViewBuilder;
  @Inject private EnvironmentElasticsearchRequestHandler environmentSearchRequestHandler;

  public static final String TYPE = "environments";
  public static final String VERSION = "0.1";
  public static final Class<Environment> SOURCE_ENTITY_CLASS = Environment.class;
  private static final String CONFIGURATION_PATH = "environment/EnvironmentSchema.json";
  private static final List<Class<? extends PersistentEntity>> SUBSCRIPTION_ENTITIES =
      ImmutableList.<Class<? extends PersistentEntity>>builder()
          .add(Application.class)
          .add(Environment.class)
          .add(AuditHeader.class)
          .add(WorkflowExecution.class)
          .add(Pipeline.class)
          .add(Workflow.class)
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
  public Class<Environment> getSourceEntityClass() {
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
    return environmentChangeHandler;
  }

  @Override
  public ElasticsearchRequestHandler getElasticsearchRequestHandler() {
    return environmentSearchRequestHandler;
  }

  @Override
  public EnvironmentView getView(Environment environment) {
    return environmentViewBuilder.createEnvironmentView(environment);
  }
}
