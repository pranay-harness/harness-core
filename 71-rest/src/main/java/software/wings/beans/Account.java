package software.wings.beans;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.encryption.Encrypted;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.security.EncryptionInterface;
import io.harness.security.SimpleEncryption;
import io.harness.validation.Create;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

@FieldNameConstants(innerTypeName = "AccountKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "accounts", noClassnameStored = true)
@HarnessEntity(exportable = true)
@Indexes({
  @Index(options = @IndexOptions(name = "next_iteration_license_info2"),
      fields = { @Field("licenseExpiryCheckIteration")
                 , @Field("encryptedLicenseInfo") })
  ,
      @Index(options = @IndexOptions(name = "next_iteration_git_sync_error"),
          fields = { @Field("gitSyncExpiryCheckIteration") })
})

public class Account extends Base implements PersistentRegularIterable {
  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  @NotNull private String companyName;

  @Indexed(options = @IndexOptions(unique = true)) @NotNull private String accountName;

  private Set<String> whitelistedDomains = new HashSet<>();

  @JsonIgnore @NotNull(groups = Create.class) @Encrypted(fieldName = "account_key") private String accountKey;

  private String licenseId;

  private List<String> salesContacts;

  private LicenseInfo licenseInfo;

  private Set<AccountEvent> accountEvents;

  @Getter @Setter private String subdomainUrl;

  @JsonIgnore private byte[] encryptedLicenseInfo;

  @JsonIgnore private boolean emailSentToSales;

  @Getter(onMethod = @__({ @JsonIgnore }))
  @Setter(onMethod = @__({ @JsonIgnore }))
  @JsonIgnore
  private long lastLicenseExpiryReminderSentAt;

  @JsonIgnore private EncryptionInterface encryption;
  private boolean twoFactorAdminEnforced;

  // Set this flag when creating an empty account for data import.
  // It's a transient field and won't be persisted.
  @Transient private boolean forImport;

  @Getter @Setter private String migratedToClusterUrl;

  /**
   * If this flag is set, all encryption/decryption activities will go through LOCAL security manager.
   * No VAULT/KMS secret manager can be configured. This helps for accounts whose delegate can't access
   * internet KMS. This setting is typically needed for some on-prem installations.
   */
  private boolean localEncryptionEnabled;

  private DelegateConfiguration delegateConfiguration;

  private Set<TechStack> techStacks;

  private boolean oauthEnabled;

  @Indexed private Long serviceGuardDataCollectionIteration;
  @Indexed private Long serviceGuardDataAnalysisIteration;
  @Indexed private Long workflowDataCollectionIteration;
  @Indexed private Long usageMetricsTaskIteration;
  @Indexed private Long licenseExpiryCheckIteration;
  @Indexed private Long gitSyncExpiryCheckIteration;
  @Indexed private Long secretManagerValidationIterator;
  private boolean cloudCostEnabled;

  private transient Map<String, String> defaults = new HashMap<>();
  /**
   * Default mechanism is USER_PASSWORD
   */
  @JsonIgnore private AuthenticationMechanism authenticationMechanism = AuthenticationMechanism.USER_PASSWORD;

  public Map<String, String> getDefaults() {
    return defaults;
  }

  public void setDefaults(Map<String, String> defaults) {
    this.defaults = defaults;
  }

  public boolean isTwoFactorAdminEnforced() {
    return twoFactorAdminEnforced;
  }

  public void setTwoFactorAdminEnforced(boolean twoFactorAdminEnforced) {
    this.twoFactorAdminEnforced = twoFactorAdminEnforced;
  }

  public boolean isForImport() {
    return forImport;
  }

  public void setForImport(boolean forImport) {
    this.forImport = forImport;
  }

  public boolean isLocalEncryptionEnabled() {
    return localEncryptionEnabled;
  }

  public void setLocalEncryptionEnabled(boolean localEncryptionEnabled) {
    this.localEncryptionEnabled = localEncryptionEnabled;
  }

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

  public LicenseInfo getLicenseInfo() {
    return licenseInfo;
  }

  public void setLicenseInfo(LicenseInfo licenseInfo) {
    this.licenseInfo = licenseInfo;
  }

  @JsonIgnore
  public byte[] getEncryptedLicenseInfo() {
    if (encryptedLicenseInfo != null) {
      return Arrays.copyOf(encryptedLicenseInfo, encryptedLicenseInfo.length);
    }

    return null;
  }

  @JsonIgnore
  public void setEncryptedLicenseInfo(byte[] encryptedLicenseInfo) {
    if (encryptedLicenseInfo != null) {
      this.encryptedLicenseInfo = Arrays.copyOf(encryptedLicenseInfo, encryptedLicenseInfo.length);
    }
  }

  public EncryptionInterface getEncryption() {
    return encryption;
  }

  public void setEncryption(EncryptionInterface encryption) {
    this.encryption = encryption;
  }

  public AuthenticationMechanism getAuthenticationMechanism() {
    return authenticationMechanism;
  }

  public void setAuthenticationMechanism(AuthenticationMechanism authenticationMechanism) {
    this.authenticationMechanism = authenticationMechanism;
  }

  @JsonIgnore
  public boolean isCommunity() {
    return licenseInfo != null && AccountType.isCommunity(licenseInfo.getAccountType());
  }

  public DelegateConfiguration getDelegateConfiguration() {
    return delegateConfiguration;
  }

  public void setDelegateConfiguration(DelegateConfiguration delegateConfiguration) {
    this.delegateConfiguration = delegateConfiguration;
  }

  @JsonIgnore
  public List<String> getSalesContacts() {
    return salesContacts;
  }

  @JsonIgnore
  public void setSalesContacts(List<String> salesContacts) {
    this.salesContacts = salesContacts;
  }

  @JsonIgnore
  public boolean isEmailSentToSales() {
    return emailSentToSales;
  }

  @JsonIgnore
  public void setEmailSentToSales(boolean emailSentToSales) {
    this.emailSentToSales = emailSentToSales;
  }

  public Set<String> getWhitelistedDomains() {
    return whitelistedDomains;
  }

  public void setWhitelistedDomains(Set<String> whitelistedDomains) {
    this.whitelistedDomains = whitelistedDomains;
  }

  public Set<TechStack> getTechStacks() {
    return techStacks;
  }

  public void setTechStacks(Set<TechStack> techStacks) {
    this.techStacks = techStacks;
  }

  public boolean isOauthEnabled() {
    return this.oauthEnabled;
  }

  public void setOauthEnabled(boolean oauthEnabled) {
    this.oauthEnabled = oauthEnabled;
  }

  public boolean isCloudCostEnabled() {
    return this.cloudCostEnabled;
  }

  public void setCloudCostEnabled(boolean cloudCostEnabled) {
    this.cloudCostEnabled = cloudCostEnabled;
  }

  public Set<AccountEvent> getAccountEvents() {
    return accountEvents;
  }

  public void setEvents(Set<AccountEvent> events) {
    this.accountEvents = events;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

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
        + "companyName='" + companyName + '\'' + ", accountName='" + accountName + '\'' + '}';
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (AccountKeys.serviceGuardDataCollectionIteration.equals(fieldName)) {
      this.serviceGuardDataCollectionIteration = nextIteration;
      return;
    }

    else if (AccountKeys.serviceGuardDataAnalysisIteration.equals(fieldName)) {
      this.serviceGuardDataAnalysisIteration = nextIteration;
      return;
    }

    else if (AccountKeys.workflowDataCollectionIteration.equals(fieldName)) {
      this.workflowDataCollectionIteration = nextIteration;
      return;
    }

    else if (AccountKeys.usageMetricsTaskIteration.equals(fieldName)) {
      this.usageMetricsTaskIteration = nextIteration;
      return;
    }

    else if (AccountKeys.licenseExpiryCheckIteration.equals(fieldName)) {
      this.licenseExpiryCheckIteration = nextIteration;
      return;
    }

    else if (AccountKeys.gitSyncExpiryCheckIteration.equals(fieldName)) {
      this.gitSyncExpiryCheckIteration = nextIteration;
      return;
    }

    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (AccountKeys.serviceGuardDataCollectionIteration.equals(fieldName)) {
      return this.serviceGuardDataCollectionIteration;
    }

    else if (AccountKeys.serviceGuardDataAnalysisIteration.equals(fieldName)) {
      return this.serviceGuardDataAnalysisIteration;
    }

    else if (AccountKeys.workflowDataCollectionIteration.equals(fieldName)) {
      return this.workflowDataCollectionIteration;
    }

    else if (AccountKeys.usageMetricsTaskIteration.equals(fieldName)) {
      return this.usageMetricsTaskIteration;
    }

    else if (AccountKeys.licenseExpiryCheckIteration.equals(fieldName)) {
      return this.licenseExpiryCheckIteration;
    }

    else if (AccountKeys.gitSyncExpiryCheckIteration.equals(fieldName)) {
      return this.gitSyncExpiryCheckIteration;
    }

    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
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
    private AuthenticationMechanism authenticationMechanism;
    private DelegateConfiguration delegateConfiguration;
    private Map<String, String> defaults = new HashMap<>();
    private LicenseInfo licenseInfo;
    private boolean emailSentToSales;
    private Set<String> whitelistedDomains;
    private long lastLicenseExpiryReminderSentAt;
    private boolean oauthEnabled;
    private boolean cloudCostEnabled;
    private String subdomainUrl;

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

    public Builder withWhitelistedDomains(Set<String> whitelistedDomains) {
      this.whitelistedDomains = whitelistedDomains;
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

    public Builder withAuthenticationMechanism(AuthenticationMechanism mechanism) {
      this.authenticationMechanism = mechanism;
      return this;
    }

    public Builder withDelegateConfiguration(DelegateConfiguration delegateConfiguration) {
      this.delegateConfiguration = delegateConfiguration;
      return this;
    }

    public Builder withDefaults(Map<String, String> defaults) {
      this.defaults = defaults;
      return this;
    }

    public Builder withLicenseInfo(LicenseInfo licenseInfo) {
      this.licenseInfo = licenseInfo;
      return this;
    }

    public Builder withEmailSentToSales(boolean emailSentToSales) {
      this.emailSentToSales = emailSentToSales;
      return this;
    }

    public Builder withLastLicenseExpiryReminderSentAt(long lastLicenseExpiryReminderSentAt) {
      this.lastLicenseExpiryReminderSentAt = lastLicenseExpiryReminderSentAt;
      return this;
    }

    public Builder withOauthEnabled(boolean oauthEnabled) {
      this.oauthEnabled = oauthEnabled;
      return this;
    }

    public Builder withCloudCostEnabled(boolean cloudCostEnabled) {
      this.cloudCostEnabled = cloudCostEnabled;
      return this;
    }

    public Builder withSubdomainUrl(String subdomainUrl) {
      this.subdomainUrl = subdomainUrl;
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
          .withLastUpdatedAt(lastUpdatedAt)
          .withAuthenticationMechanism(authenticationMechanism)
          .withDelegateConfiguration(delegateConfiguration)
          .withDefaults(defaults)
          .withLicenseInfo(licenseInfo)
          .withEmailSentToSales(emailSentToSales)
          .withWhitelistedDomains(whitelistedDomains)
          .withLastLicenseExpiryReminderSentAt(lastLicenseExpiryReminderSentAt)
          .withOauthEnabled(oauthEnabled)
          .withSubdomainUrl(subdomainUrl);
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
      account.setAuthenticationMechanism(authenticationMechanism);
      account.setDelegateConfiguration(delegateConfiguration);
      account.setDefaults(defaults);
      account.setLicenseInfo(licenseInfo);
      account.setEmailSentToSales(emailSentToSales);
      account.setWhitelistedDomains(whitelistedDomains);
      account.setLastLicenseExpiryReminderSentAt(lastLicenseExpiryReminderSentAt);
      account.setOauthEnabled(oauthEnabled);
      account.setCloudCostEnabled(cloudCostEnabled);
      account.setSubdomainUrl(subdomainUrl);
      return account;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseEntityYaml {
    private List<NameValuePair.Yaml> defaults = new ArrayList<>();

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, List<NameValuePair.Yaml> defaults) {
      super(type, harnessApiVersion);
      this.defaults = defaults;
    }
  }

  @UtilityClass
  public static final class AccountKeys {
    // Temporary
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
    public static final String name = "name";
    public static final String licenseExpiryCheckIteration = "licenseExpiryCheckIteration";
    public static final String subdomainUrl = "subdomainUrl";
    public static final String gitSyncExpiryCheckIteration = "gitSyncExpiryCheckIteration";
  }
}
