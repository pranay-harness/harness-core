package software.wings.search.entities.application;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.persistence.PersistentEntity;
import lombok.extern.slf4j.Slf4j;
import software.wings.audit.AuditHeader;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.ElasticsearchRequestHandler;
import software.wings.search.framework.SearchEntity;

import java.util.List;

@Slf4j
public class ApplicationSearchEntity implements SearchEntity<Application> {
  @Inject private ApplicationChangeHandler applicationChangeHandler;
  @Inject private ApplicationViewBuilder applicationViewBuilder;
  @Inject private ApplicationElasticsearchRequestHandler applicationElasticsearchRequestHandler;

  public static final String TYPE = "applications";
  public static final String VERSION = "0.2";
  public static final Class<Application> SOURCE_ENTITY_CLASS = Application.class;
  private static final List<Class<? extends PersistentEntity>> SUBSCRIPTION_ENTITIES =
      ImmutableList.<Class<? extends PersistentEntity>>builder()
          .add(Application.class)
          .add(AuditHeader.class)
          .add(Workflow.class)
          .add(Pipeline.class)
          .add(Environment.class)
          .add(Service.class)
          .build();
  private static final String CONFIGURATION_PATH = "application/ApplicationSchema.json";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Class<Application> getSourceEntityClass() {
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
    return applicationChangeHandler;
  }

  @Override
  public ElasticsearchRequestHandler getElasticsearchRequestHandler() {
    return applicationElasticsearchRequestHandler;
  }

  @Override
  public ApplicationView getView(Application application) {
    return applicationViewBuilder.createApplicationView(application, false);
  }
}
