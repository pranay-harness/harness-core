package software.wings.search.entities.environment;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Environment;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

@Slf4j
public class EnvironmentSearchEntity implements SearchEntity<Environment> {
  @Inject private EnvironmentChangeHandler environmentChangeHandler;
  @Inject private EnvironmentViewBuilder environmentViewBuilder;

  public static final String TYPE = "environments";
  public static final String VERSION = "0.1";
  public static final Class<Environment> SOURCE_ENTITY_CLASS = Environment.class;
  private static final String CONFIGURATION_PATH = "environment/EnvironmentSchema.json";

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
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return environmentChangeHandler;
  }

  @Override
  public EnvironmentView getView(Environment environment) {
    return environmentViewBuilder.createEnvironmentView(environment);
  }
}
