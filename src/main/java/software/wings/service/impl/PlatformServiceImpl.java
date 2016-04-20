package software.wings.service.impl;

import com.google.inject.Singleton;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.PlatformSoftware;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PlatformService;

import javax.inject.Inject;

@Singleton
public class PlatformServiceImpl implements PlatformService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<PlatformSoftware> list(PageRequest<PlatformSoftware> req) {
    return wingsPersistence.query(PlatformSoftware.class, req);
  }

  @Override
  public PlatformSoftware create(PlatformSoftware platform) {
    return wingsPersistence.saveAndGet(PlatformSoftware.class, platform);
  }

  @Override
  public PlatformSoftware update(PlatformSoftware platform) {
    return null;
  }
}
