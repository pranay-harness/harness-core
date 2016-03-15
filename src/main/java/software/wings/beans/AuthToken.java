package software.wings.beans;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.utils.CryptoUtil;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

/**
 * Created by anubhaw on 3/14/16.
 */

@Entity
public class AuthToken {
  @Id private ObjectId id;

  private String token;
  private String userID;
  private long createdAt;
  private long expireAt;

  public AuthToken(String userID) {
    this.userID = userID;
    token = secureRandAlphaNumString(32);
    createdAt = System.currentTimeMillis();
    expireAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000; // 24 hrs expiry
  }

  public String getToken() {
    return token;
  }
  public void setToken(String token) {
    this.token = token;
  }
  public String getUserID() {
    return userID;
  }
  public void setUserID(String userID) {
    this.userID = userID;
  }
  public long getCreatedAt() {
    return createdAt;
  }
  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
  public long getExpireAt() {
    return expireAt;
  }
  public void setExpireAt(long expireAt) {
    this.expireAt = expireAt;
  }
}
