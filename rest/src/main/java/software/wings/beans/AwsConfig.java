package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 12/27/16.
 */
@JsonTypeName("AWS")
public class AwsConfig extends SettingValue {
  @Attributes(title = "Access Key") private String accessKey;
  @Attributes(title = "Secret Key") private String secretKey;

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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accessKey;
    private String secretKey;

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
      return anAwsConfig().withAccessKey(accessKey).withSecretKey(secretKey);
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
      return awsConfig;
    }
  }
}
