package software.wings.beans;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.utils.CryptoUtils.secureRandAlphaNumString;

import io.harness.annotation.HarnessEntity;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by rsingh on 06/21/17.
 */
@Entity(value = "externalServiceAuthTokens", noClassnameStored = true)
@HarnessEntity(exportable = false)
public final class ExternalServiceAuthToken extends Base {
  private long expireAt;

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && true;
  }

  /**
   * Instantiates a new auth token.
   *
   * @param tokenExpiryInMillis the token expiry in millis
   */
  public ExternalServiceAuthToken(Long tokenExpiryInMillis) {
    setUuid(secureRandAlphaNumString(32));
    setAppId(GLOBAL_APP_ID);
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
