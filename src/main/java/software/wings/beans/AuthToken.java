package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

import javax.validation.constraints.NotNull;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/14/16.
 */
@Entity(value = "authTokens", noClassnameStored = true)
public class AuthToken extends Base {
  @Reference(idOnly = true) @NotNull private User user;

  private long expireAt;

  /**
   * Instantiates a new auth token.
   */
  public AuthToken() {}

  /**
   * Instantiates a new auth token.
   *
   * @param user the user
   */
  public AuthToken(User user) {
    this.user = user;
    setUuid(secureRandAlphaNumString(32));
    setAppId(Base.GLOBAL_APP_ID);
    expireAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 24 hrs expiry
  }

  /**
   * Gets user.
   *
   * @return the user
   */
  public User getUser() {
    return user;
  }

  /**
   * Sets user.
   *
   * @param user the user
   */
  public void setUser(User user) {
    this.user = user;
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
}
