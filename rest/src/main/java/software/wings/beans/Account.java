package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.utils.validation.Create;

import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Entity(value = "accounts", noClassnameStored = true)
public class Account extends Base {
  @Indexed(unique = true) @NotNull private String companyName;

  @NotNull(groups = Create.class) private String accountKey;

  /**
   * Getter for property 'companyName'.
   *
   * @return Value for property 'companyName'.
   */
  public String getCompanyName() {
    return companyName;
  }

  /**
   * Setter for property 'companyName'.
   *
   * @param companyName Value to set for property 'companyName'.
   */
  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  /**
   * Getter for property 'accountKey'.
   *
   * @return Value for property 'accountKey'.
   */
  public String getAccountKey() {
    return accountKey;
  }

  /**
   * Setter for property 'accountKey'.
   *
   * @param accountKey Value to set for property 'accountKey'.
   */
  public void setAccountKey(String accountKey) {
    this.accountKey = accountKey;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(companyName, accountKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final Account other = (Account) obj;
    return Objects.equals(this.companyName, other.companyName) && Objects.equals(this.accountKey, other.accountKey);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("companyName", companyName).add("accountKey", accountKey).toString();
  }

  public static final class Builder {
    private String companyName;
    private String accountKey;
    private String uuid;
    private String appId = GLOBAL_APP_ID;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder anAccount() {
      return new Builder();
    }

    public Builder withCompanyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    public Builder withAccountKey(String accountKey) {
      this.accountKey = accountKey;
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

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder but() {
      return anAccount()
          .withCompanyName(companyName)
          .withAccountKey(accountKey)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public Account build() {
      Account account = new Account();
      account.setCompanyName(companyName);
      account.setAccountKey(accountKey);
      account.setUuid(uuid);
      account.setAppId(appId);
      account.setCreatedBy(createdBy);
      account.setCreatedAt(createdAt);
      account.setLastUpdatedBy(lastUpdatedBy);
      account.setLastUpdatedAt(lastUpdatedAt);
      return account;
    }
  }
}
