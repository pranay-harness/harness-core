package software.wings.beans.infrastructure;

import static software.wings.beans.SettingValue.SettingVariableTypes.AWS_CREDENTIALS;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.Objects;

/**
 * Created by anubhaw on 10/4/16.
 */
@JsonTypeName("AWS_CREDENTIALS")
public class AwsInfrastructureProviderConfig extends InfrastructureProviderConfig {
  private String accessKey;
  private String secretKey;

  /**
   * Instantiates a new setting value.
   */
  public AwsInfrastructureProviderConfig() {
    super(AWS_CREDENTIALS);
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
  public String getSecretKey() {
    return secretKey;
  }

  /**
   * Sets secret key.
   *
   * @param secretKey the secret key
   */
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessKey, secretKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final AwsInfrastructureProviderConfig other = (AwsInfrastructureProviderConfig) obj;
    return Objects.equals(this.accessKey, other.accessKey) && Objects.equals(this.secretKey, other.secretKey);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("accessKey", accessKey).add("secretKey", secretKey).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accessKey;
    private String secretKey;

    private Builder() {}

    /**
     * An aws infrastructure provider config builder.
     *
     * @return the builder
     */
    public static Builder anAwsInfrastructureProviderConfig() {
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
    public Builder withSecretKey(String secretKey) {
      this.secretKey = secretKey;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsInfrastructureProviderConfig().withAccessKey(accessKey).withSecretKey(secretKey);
    }

    /**
     * Build aws infrastructure provider config.
     *
     * @return the aws infrastructure provider config
     */
    public AwsInfrastructureProviderConfig build() {
      AwsInfrastructureProviderConfig awsInfrastructureProviderConfig = new AwsInfrastructureProviderConfig();
      awsInfrastructureProviderConfig.setAccessKey(accessKey);
      awsInfrastructureProviderConfig.setSecretKey(secretKey);
      return awsInfrastructureProviderConfig;
    }
  }
}
