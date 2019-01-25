package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.simpleframework.xml.Transient;

import java.time.OffsetDateTime;
import java.util.Date;

@Entity(value = "authTokens", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthToken extends Base {
  @Transient private User user;
  private String userId;
  private long expireAt;
  private String jwtToken;
  private boolean refreshed;

  // TODO: remove this a after February 10th 2019
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0)) private Date ttl;

  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(7).toInstant());

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
}
