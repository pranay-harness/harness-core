package software.wings.service;

import javax.inject.Inject;

import org.mindrot.jbcrypt.BCrypt;

import software.wings.beans.User;
import software.wings.dl.WingsPersistence;

/**
 * Created by anubhaw on 3/9/16.
 */
public class UserService {
  @Inject private WingsPersistence wingsPersistence;

  public User register(User user) {
    String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
    user.setPasswordHash(hashed);
    return wingsPersistence.saveAndGet(User.class, user);
  }

  public boolean matchPassword(String password, String hash) {
    return BCrypt.checkpw(password, hash);
  }
}
