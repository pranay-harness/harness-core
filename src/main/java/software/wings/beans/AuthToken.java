package software.wings.beans;

/**
 * Created by anubhaw on 3/14/16.
 */

public class AuthToken {
  private String token;
  private String userID;
  private long createdAt;
  private long expireAt;

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
