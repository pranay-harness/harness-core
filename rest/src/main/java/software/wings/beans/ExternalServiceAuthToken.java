package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by rsingh on 06/21/17.
 */
@Entity(value = "externalServiceAuthTokens", noClassnameStored = true)
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
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
