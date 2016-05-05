package software.wings.service.impl;

import com.google.inject.Singleton;

import software.wings.beans.AppContainer;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PlatformService;

import javax.inject.Inject;

@Singleton
public class PlatformServiceImpl implements PlatformService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<AppContainer> list(PageRequest<AppContainer> req) {
    return wingsPersistence.query(AppContainer.class, req);
  }

  @Override
  public AppContainer create(AppContainer platform) {
    return wingsPersistence.saveAndGet(AppContainer.class, platform);
  }

  @Override
  public AppContainer update(AppContainer platform) {
    return null;
  }
}
