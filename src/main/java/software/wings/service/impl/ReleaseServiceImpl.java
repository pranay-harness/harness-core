package software.wings.service.impl;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;

import software.wings.beans.Application;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Release;
import software.wings.dl.MongoHelper;
import software.wings.service.intfc.ReleaseService;

public class ReleaseServiceImpl implements ReleaseService {
  private Datastore datastore;

  public ReleaseServiceImpl(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public Release create(Release release) {
    Key<Release> key = datastore.save(release);
    return datastore.get(Release.class, key.getId());
  }

  @Override
  public Release update(Release release) {
    Key<Release> key = datastore.save(release);
    return datastore.get(Release.class, key.getId());
  }

  @Override
  public PageResponse<Release> list(PageRequest<Release> req) {
    return MongoHelper.queryPageRequest(datastore, Release.class, req);
  }
}
