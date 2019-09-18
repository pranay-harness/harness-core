package software.wings.search.entities.service;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.ChangeHandler;
import software.wings.search.framework.SearchEntity;

@Slf4j
public class ServiceSearchEntity implements SearchEntity<Service> {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceChangeHandler serviceChangeHandler;

  public static final String TYPE = "services";
  public static final String VERSION = "0.1";
  public static final Class<Service> SOURCE_ENTITY_CLASS = Service.class;
  private static final String CONFIGURATION_PATH = "service/ServiceSchema.json";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public Class<Service> getSourceEntityClass() {
    return SOURCE_ENTITY_CLASS;
  }

  @Override
  public String getConfigurationPath() {
    return CONFIGURATION_PATH;
  }

  @Override
  public ChangeHandler getChangeHandler() {
    return serviceChangeHandler;
  }

  @Override
  public ServiceView getView(Service service) {
    ServiceView serviceView = ServiceView.fromService(service);
    Application application = wingsPersistence.get(Application.class, service.getAppId());
    serviceView.setAppName(application.getName());
    return serviceView;
  }
}
