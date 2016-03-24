package software.wings.service;

import org.bson.Document;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.*;
import software.wings.dl.MongoHelper;

/**
 * Created by anubhaw on 3/23/16.
 */

public class RoleService {
  private Datastore datastore;
  private static Logger logger = LoggerFactory.getLogger(RoleService.class);

  public RoleService(Datastore datastore) {
    this.datastore = datastore;
  }

  public PageResponse<Role> list(PageRequest<Role> pageRequest) {
    return MongoHelper.queryPageRequest(datastore, Role.class, pageRequest);
  }

  public Role save(Role role) {
    Key<Role> key = datastore.save(role);
    logger.debug("Key of the saved entity :" + key.toString());
    return datastore.get(Role.class, role.getUuid());
  }

  public Role findByUUID(String uuid) {
    return datastore.get(Role.class, uuid);
  }

  public Role update(Role role) {
    return save(role);
  }
}
