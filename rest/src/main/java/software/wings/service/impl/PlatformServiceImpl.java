package software.wings.service.impl;

import com.google.inject.Singleton;

import software.wings.beans.AppContainer;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PlatformService;

import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * The Class PlatformServiceImpl.
 */
@ValidateOnExecution
@Singleton
public class PlatformServiceImpl implements PlatformService {
  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.PlatformService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<AppContainer> list(PageRequest<AppContainer> req) {
    return wingsPersistence.query(AppContainer.class, req);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.PlatformService#create(software.wings.beans.AppContainer)
   */
  @Override
  public AppContainer create(AppContainer platform) {
    return wingsPersistence.saveAndGet(AppContainer.class, platform);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.PlatformService#update(software.wings.beans.AppContainer)
   */
  @Override
  public AppContainer update(AppContainer platform) {
    return null;
  }
}
