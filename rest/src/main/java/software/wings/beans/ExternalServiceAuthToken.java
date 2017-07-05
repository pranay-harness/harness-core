package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import org.mongodb.morphia.annotations.Entity;

import java.util.UUID;

/**
 * Created by rsingh on 06/21/17.
 */
@Entity(value = "externalServiceAuthTokens", noClassnameStored = true)
public class ExternalServiceAuthToken extends Base {
  private long expireAt;

  /**
   * Instantiates a new auth token.
   *
   * @param tokenExpiryInMillis the token expiry in millis
   */
  public ExternalServiceAuthToken(Long tokenExpiryInMillis) {
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
}
