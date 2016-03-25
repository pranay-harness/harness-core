package software.wings.service;

import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Role;
import software.wings.beans.User;
import software.wings.dl.MongoHelper;
import software.wings.exception.WingsException;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

/**
 * Created by anubhaw on 3/9/16.
 */
public class UserService {
  private Datastore datastore;

  public UserService(Datastore datastore) {
    this.datastore = datastore;
  }

  public User register(User user) {
    String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
    user.setPasswordHash(hashed);
    Key<User> key = datastore.save(user);
    return datastore.get(User.class, key.getId());
  }

  public boolean matchPassword(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }

  public User addRole(String userID, String roleID) {
    User user = datastore.get(User.class, userID);
    Role role = datastore.get(Role.class, roleID);
    if (user != null && role != null) {
      UpdateOperations<User> updateOp = datastore.createUpdateOperations(User.class).add("roles", role);
      Query<User> updateQuery = datastore.createQuery(User.class).field(ID_KEY).equal(userID);
      datastore.update(updateQuery, updateOp);
      return datastore.get(User.class, userID);
    }
    throw new WingsException("Invalid operation. Either User or Role doesn't exist");
  }

  public User update(User user) {
    return register(user);
  }

  public PageResponse<User> list(PageRequest<User> pageRequest) {
    return MongoHelper.queryPageRequest(datastore, User.class, pageRequest);
  }

  public void delete(String userID) {
    datastore.delete(User.class, userID);
  }

  public User get(String userID) {
    return datastore.get(User.class, userID);
  }

  public User revokeRole(String userID, String roleID) {
    Role role = new Role();
    role.setUuid(roleID);
    UpdateOperations<User> updateOp = datastore.createUpdateOperations(User.class).removeAll("roles", role);
    Query<User> updateQuery = datastore.createQuery(User.class).field(ID_KEY).equal(userID);
    datastore.update(updateQuery, updateOp);
    return datastore.get(User.class, userID);
  }
}
