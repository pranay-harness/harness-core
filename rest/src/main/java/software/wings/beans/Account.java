package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.EncryptionInterface;
import software.wings.security.encryption.SimpleEncryption;
import software.wings.utils.validation.Create;

import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 10/11/16.
 */
@Entity(value = "accounts", noClassnameStored = true)
public class Account extends Base {
  @Indexed @NotNull private String companyName;

  @Indexed(unique = true) @NotNull private String accountName;

  @NotNull(groups = Create.class) @Encrypted private String accountKey;

  private String licenseId;

  private long licenseExpiryTime;

  @JsonIgnore private EncryptionInterface encryption;

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

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  /**
   * Getter for property 'licenseId'.
   *
   * @return Value for property 'licenseId'.
   */
  public String getLicenseId() {
    return licenseId;
  }

  /**
   * Setter for property 'licenseId'.
   *
   * @param licenseId Value to set for property 'licenseId'.
   */
  public void setLicenseId(String licenseId) {
    this.licenseId = licenseId;
  }

  /**
   * Getter for property 'licenseExpiryTime'.
   *
   * @return Value for property 'licenseExpiryTime'.
   */
  public long getLicenseExpiryTime() {
    return licenseExpiryTime;
  }

  /**
   * Setter for property 'licenseExpiryTime'.
   *
   * @param licenseExpiryTime Value to set for property 'licenseExpiryTime'.
   */
  public void setLicenseExpiryTime(long licenseExpiryTime) {
    this.licenseExpiryTime = licenseExpiryTime;
  }

  public EncryptionInterface getEncryption() {
    return encryption;
  }

  public void setEncryption(EncryptionInterface encryption) {
    this.encryption = encryption;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    Account account = (Account) o;

    return accountName != null ? accountName.equals(account.accountName) : account.accountName == null;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (accountName != null ? accountName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Account{"
        + "companyName='" + companyName + '\'' + ", accountName='" + accountName + '\'' + ", accountKey='" + accountKey
        + '\'' + '}';
  }

  public static final class Builder {
    private String companyName;
    private String accountName;
    private String accountKey;
    private String uuid;
    private String appId = GLOBAL_APP_ID;
    private EncryptionInterface encryption = new SimpleEncryption();
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

    public Builder withAccountName(String accountName) {
      this.accountName = accountName;
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

    public Builder withEncryption(EncryptionInterface encryption) {
      this.encryption = encryption;
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
          .withEncryption(encryption)
          .withCreatedBy(createdBy)
          .withAccountName(accountName)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public Account build() {
      Account account = new Account();
      account.setCompanyName(companyName);
      account.setAccountName(accountName);
      account.setAccountKey(accountKey);
      account.setUuid(uuid);
      account.setAppId(appId);
      account.setEncryption(encryption);
      account.setCreatedBy(createdBy);
      account.setCreatedAt(createdAt);
      account.setLastUpdatedBy(lastUpdatedBy);
      account.setLastUpdatedAt(lastUpdatedAt);
      return account;
    }
  }
}
