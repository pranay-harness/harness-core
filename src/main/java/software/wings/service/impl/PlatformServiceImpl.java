package software.wings.service.impl;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;

import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.PlatformSoftware;
import software.wings.dl.MongoHelper;
import software.wings.service.intfc.PlatformService;

public class PlatformServiceImpl implements PlatformService {
  private Datastore datastore;

  public PlatformServiceImpl(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public PageResponse<PlatformSoftware> list(PageRequest<PlatformSoftware> req) {
    return MongoHelper.queryPageRequest(datastore, PlatformSoftware.class, req);
  }

  @Override
  public PlatformSoftware create(PlatformSoftware platform) {
    Key<PlatformSoftware> key = datastore.save(platform);
    return datastore.get(PlatformSoftware.class, key.getId());
  }

  @Override
  public PlatformSoftware update(PlatformSoftware platform) {
    return null;
  }
}
