package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.UserService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 3/9/16.
 */
public class UserServiceImpl implements UserService {
  @Inject private WingsPersistence wingsPersistence;

  public User register(User user) {
    String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
    user.setPasswordHash(hashed);
    return wingsPersistence.saveAndGet(User.class, user);
  }

  public boolean matchPassword(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }

  public User addRole(String userId, String roleId) {
    User user = wingsPersistence.get(User.class, userId);
    Role role = wingsPersistence.get(Role.class, roleId);
    if (user != null && role != null) {
      UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).add("roles", role);
      Query<User> updateQuery = wingsPersistence.createQuery(User.class).field(ID_KEY).equal(userId);
      wingsPersistence.update(updateQuery, updateOp);
      return wingsPersistence.get(User.class, userId);
    }
    throw new WingsException(
        "Invalid operation. Either User or Role doesn't exist user = [" + user + "] role = [" + role + "]");
  }

  public User update(User user) {
    return register(user);
  }

  public PageResponse<User> list(PageRequest<User> pageRequest) {
    return wingsPersistence.query(User.class, pageRequest);
  }

  public void delete(String userID) {
    wingsPersistence.delete(User.class, userID);
  }

  public User get(String userID) {
    return wingsPersistence.get(User.class, userID);
  }

  public User revokeRole(String userID, String roleID) {
    Role role = new Role();
    role.setUuid(roleID);
    UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).removeAll("roles", role);
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).field(ID_KEY).equal(userID);
    wingsPersistence.update(updateQuery, updateOp);
    return wingsPersistence.get(User.class, userID);
  }
}
