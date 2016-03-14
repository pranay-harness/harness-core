package software.wings.service;

import org.mindrot.jbcrypt.BCrypt;
import org.mongodb.morphia.Datastore;
import software.wings.beans.User;
import software.wings.common.UUIDGenerator;

/**
 * Created by anubhaw on 3/9/16.
 */
public class UserService {
  private Datastore datastore;

  public UserService(Datastore datastore) {
    this.datastore = datastore;
  }

  public void register(User user) {
    String hashed = BCrypt.hashpw("password", BCrypt.gensalt());
    user.setPasswordHash(hashed);
    datastore.save(user);
  }

  public boolean matchPassword(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }
}
