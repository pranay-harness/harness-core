package software.wings.service;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.WingsBootstrap;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.MongoHelper;

import java.util.List;

/**
 * Created by anubhaw on 3/23/16.
 */

public class RoleService {
  private Datastore datastore;
  private UserService userService;

  private static Logger logger = LoggerFactory.getLogger(RoleService.class);

  public RoleService(Datastore datastore, UserService userService) {
    this.datastore = datastore;
    this.userService = userService;
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

  public void delete(String roleID) {
    datastore.delete(Role.class, roleID);
    List<User> users = datastore.createQuery(User.class).disableValidation().field("roles").equal(roleID).asList();
    for (User user : users) {
      userService.revokeRole(user.getUuid(), roleID);
    }
  }
}
