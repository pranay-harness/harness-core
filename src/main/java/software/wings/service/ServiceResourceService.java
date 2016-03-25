package software.wings.service;

import com.mongodb.Mongo;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.dl.MongoHelper;

/**
 * Created by anubhaw on 3/25/16.
 */

public class ServiceResourceService {
  private Datastore datastore; // TODO: AutoInject

  public PageResponse<Service> list(PageRequest<Service> pageRequest) {
    return MongoHelper.queryPageRequest(datastore, Service.class, pageRequest);
  }

  public Service save(Service service) {
    Key<Service> key = datastore.save(service);
    return datastore.get(Service.class, key);
  }

  public Service findByUUID(String uuid) {
    return datastore.get(Service.class, uuid);
  }

  public Service update(Service service) {
    return save(service);
  }
}
