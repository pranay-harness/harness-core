package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.UserService;

import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/9/16.
 */
@ValidateOnExecution
public class UserServiceImpl implements UserService {
  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#register(software.wings.beans.User)
   */
  public User register(User user) {
    String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
    user.setPasswordHash(hashed);
    return wingsPersistence.saveAndGet(User.class, user);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#matchPassword(java.lang.String, java.lang.String)
   */
  public boolean matchPassword(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#addRole(java.lang.String, java.lang.String)
   */
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

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#update(software.wings.beans.User)
   */
  public User update(User user) {
    return register(user);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#list(software.wings.dl.PageRequest)
   */
  public PageResponse<User> list(PageRequest<User> pageRequest) {
    return wingsPersistence.query(User.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#delete(java.lang.String)
   */
  public void delete(String userId) {
    wingsPersistence.delete(User.class, userId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#get(java.lang.String)
   */
  public User get(String userId) {
    return wingsPersistence.get(User.class, userId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.UserService#revokeRole(java.lang.String, java.lang.String)
   */
  public User revokeRole(String userId, String roleId) {
    Role role = new Role();
    role.setUuid(roleId);
    UpdateOperations<User> updateOp = wingsPersistence.createUpdateOperations(User.class).removeAll("roles", role);
    Query<User> updateQuery = wingsPersistence.createQuery(User.class).field(ID_KEY).equal(userId);
    wingsPersistence.update(updateQuery, updateOp);
    return wingsPersistence.get(User.class, userId);
  }
}
