package software.wings.search.entities.application;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

@Slf4j
public class ApplicationSearchEntity implements SearchEntity<Application> {
  @Inject private ApplicationChangeHandler applicationChangeHandler;

  public static final String TYPE = "applications";
  public static final String VERSION = "0.1";
  public static final Class<Application> SOURCE_ENTITY_CLASS = Application.class;
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
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return applicationChangeHandler;
  }

  @Override
  public ApplicationView getView(Application application) {
    return ApplicationView.fromApplication(application);
  }
}
