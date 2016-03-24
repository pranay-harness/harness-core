package software.wings.service;

import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.*;
import software.wings.dl.MongoHelper;

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

  public User addRole(String userID, Role role) {
    User user = datastore.get(User.class, userID);
    if (user != null) {
      user.addRole(role);
    }
    return user;
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
    UpdateOperations<User> updateOp =
        datastore.createUpdateOperations(User.class).removeAll("roles", new Document("_id", roleID));
    Query<User> updateQuery = datastore.createQuery(User.class).field(ID_KEY).equal(userID);
    UpdateResults update = datastore.update(updateQuery, updateOp);
    return datastore.get(User.class, userID);
  }
}
