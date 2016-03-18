package software.wings.beans;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Reference;
import software.wings.utils.CryptoUtil;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

/**
 * Created by anubhaw on 3/14/16.
 */

@Entity(value = "authTokens", noClassnameStored = true)
public class AuthToken extends Base {
  @Reference(idOnly = true) private User user;
  private long expireAt;

  public AuthToken() {}

  public AuthToken(User user) {
    this.user = user;
    setUuid(secureRandAlphaNumString(32));
    expireAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 24 hrs expiry
  }

  public User getUser() {
    return user;
  }
  public void setUser(User user) {
    this.user = user;
  }
  public long getExpireAt() {
    return expireAt;
  }
  public void setExpireAt(long expireAt) {
    this.expireAt = expireAt;
  }
}
