package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 12/27/16.
 */
@JsonTypeName("AWS")
public class AwsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Access Key", required = true) @NotEmpty private String accessKey;
  @Attributes(title = "Secret Key", required = true) @NotEmpty @Encrypted private char[] secretKey;
  @SchemaIgnore @NotEmpty private String accountId; // internal

  /**
   * Instantiates a new Aws config.
   */
  public AwsConfig() {
    super(SettingVariableTypes.AWS.name());
  }

  /**
   * Gets access key.
   *
   * @return the access key
   */
  public String getAccessKey() {
    return accessKey;
  }

  /**
   * Sets access key.
   *
   * @param accessKey the access key
   */
  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  /**
   * Gets secret key.
   *
   * @return the secret key
   */
  public char[] getSecretKey() {
    return secretKey;
  }

  /**
   * Sets secret key.
   *
   * @param secretKey the secret key
   */
  public void setSecretKey(char[] secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  @Override
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accessKey;
    private char[] secretKey;
    private String accountId;

    private Builder() {}

    /**
     * An aws config builder.
     *
     * @return the builder
     */
    public static Builder anAwsConfig() {
      return new Builder();
    }

    /**
     * With access key builder.
     *
     * @param accessKey the access key
     * @return the builder
     */
    public Builder withAccessKey(String accessKey) {
      this.accessKey = accessKey;
      return this;
    }

    /**
     * With secret key builder.
     *
     * @param secretKey the secret key
     * @return the builder
     */
    public Builder withSecretKey(char[] secretKey) {
      this.secretKey = secretKey;
      return this;
    }

    /**
     * With accountId.
     *
     * @param accountId the accountId
     * @return the builder
     */
    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }
    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsConfig().withAccessKey(accessKey).withSecretKey(secretKey).withAccountId(accountId);
    }

    /**
     * Build aws config.
     *
     * @return the aws config
     */
    public AwsConfig build() {
      AwsConfig awsConfig = new AwsConfig();
      awsConfig.setAccessKey(accessKey);
      awsConfig.setSecretKey(secretKey);
      awsConfig.setAccountId(accountId);
      return awsConfig;
    }
  }
}
