package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Entity;
import org.simpleframework.xml.Transient;

/**
 * Created by anubhaw on 3/14/16.
 */
@Entity(value = "authTokens", noClassnameStored = true)
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class AuthToken extends Base {
  @Transient private User user;
  private String userId;
  private long expireAt;

  /**
   * Instantiates a new auth token.
   *
   * @param userId              the user id
   * @param tokenExpiryInMillis the token expiry in millis
   */
  public AuthToken(String userId, Long tokenExpiryInMillis) {
    this.userId = userId;
    setUuid(secureRandAlphaNumString(32));
    setAppId(Base.GLOBAL_APP_ID);
    expireAt = System.currentTimeMillis() + tokenExpiryInMillis;
  }

  /**
   * Gets expire at.
   *
   * @return the expire at
   */
  public long getExpireAt() {
    return expireAt;
  }

  /**
   * Sets expire at.
   *
   * @param expireAt the expire at
   */
  public void setExpireAt(long expireAt) {
    this.expireAt = expireAt;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }
}
