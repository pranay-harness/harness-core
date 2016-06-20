package software.wings.beans;

import static software.wings.utils.CryptoUtil.secureRandAlphaNumString;

import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 6/17/16.
 */
@Entity(value = "emailVerificationTokens")
public class EmailVerificationToken extends Base {
  private String token;
  private String userId;

  public EmailVerificationToken() {}

  public EmailVerificationToken(String userId) {
    setAppId(Base.GLOBAL_APP_ID);
    this.userId = userId;
    this.token = secureRandAlphaNumString(32);
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public static final class Builder {
    private String token;
    private String userId;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder anEmailVerificationToken() {
      return new Builder();
    }

    public Builder withToken(String token) {
      this.token = token;
      return this;
    }

    public Builder withUserId(String userId) {
      this.userId = userId;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return anEmailVerificationToken()
          .withToken(token)
          .withUserId(userId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public EmailVerificationToken build() {
      EmailVerificationToken emailVerificationToken = new EmailVerificationToken();
      emailVerificationToken.setToken(token);
      emailVerificationToken.setUserId(userId);
      emailVerificationToken.setUuid(uuid);
      emailVerificationToken.setAppId(appId);
      emailVerificationToken.setCreatedBy(createdBy);
      emailVerificationToken.setCreatedAt(createdAt);
      emailVerificationToken.setLastUpdatedBy(lastUpdatedBy);
      emailVerificationToken.setLastUpdatedAt(lastUpdatedAt);
      emailVerificationToken.setActive(active);
      return emailVerificationToken;
    }
  }
}
