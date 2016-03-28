package software.wings.service.impl;

import javax.inject.Inject;

import com.google.inject.Singleton;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ReleaseService;

@Singleton
public class ReleaseServiceImpl implements ReleaseService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Release create(Release release) {
    return wingsPersistence.saveAndGet(Release.class, release);
  }

  @Override
  public Release update(Release release) {
    String key = wingsPersistence.save(release);
    return wingsPersistence.get(Release.class, key);
  }

  @Override
  public PageResponse<Release> list(PageRequest<Release> req) {
    return wingsPersistence.query(Release.class, req);
  }
}
