package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.simpleframework.xml.Transient;

import java.util.Date;

/**
 * Created by anubhaw on 3/14/16.
 */
@Entity(value = "authTokens", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthToken extends Base {
  @Transient private User user;
  private String userId;
  private long expireAt;
  private String jwtToken;
  private boolean refreshed;
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date ttl;

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
    ttl = new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000);
  }
}
