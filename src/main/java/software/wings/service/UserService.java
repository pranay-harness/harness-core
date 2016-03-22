package software.wings.service;

import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import software.wings.beans.User;

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
}
