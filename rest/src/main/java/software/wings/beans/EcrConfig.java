package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Arrays;

/**
 * ECR Artifact Server / Connector has been deprecated.
 * Instead, we use AWS cloud provider to fetch all the connection details.
 * This class is not deleted since there might be existing configs in the mongo db.
 * We can only delete this class when the entries are migrated to use cloud provider.
 * Created by brett on 7/16/17
 */
@JsonTypeName("ECR")
@Deprecated
public class EcrConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Amazon ECR Registry URL", required = true) @NotEmpty private String ecrUrl;
  @Attributes(title = "Access Key", required = true) @NotEmpty private String accessKey;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Secret Key", required = true)
  @NotEmpty
  @Encrypted
  private char[] secretKey;
  @Attributes(title = "Region", required = true)
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region;
  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new ECR registry config.
   */
  public EcrConfig() {
    super(SettingVariableTypes.ECR.name());
  }

  /**
   * Gets docker registry url.
   *
   * @return the docker registry url
   */
  public String getEcrUrl() {
    return ecrUrl;
  }

  /**
   * Sets docker registry url.
   *
   * @param ecrUrl the docker registry url
   */
  public void setEcrUrl(String ecrUrl) {
    this.ecrUrl = ecrUrl;
  }

  /**
   * Gets accessKey.
   *
   * @return the accessKey
   */
  public String getAccessKey() {
    return accessKey;
  }

  /**
   * Sets accessKey.
   *
   * @param accessKey the accessKey
   */
  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  /**
   * Gets secretKey.
   *
   * @return the secretKey
   */
  public char[] getSecretKey() {
    return secretKey;
  }

  /**
   * Sets secretKey.
   *
   * @param secretKey the secretKey
   */
  public void setSecretKey(char[] secretKey) {
    this.secretKey = secretKey;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    EcrConfig that = (EcrConfig) o;

    if (!ecrUrl.equals(that.ecrUrl))
      return false;
    if (!accessKey.equals(that.accessKey))
      return false;
    if (!Arrays.equals(secretKey, that.secretKey))
      return false;
    if (!region.equals(that.region))
      return false;
    return accountId.equals(that.accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(ecrUrl, accessKey, secretKey, region, accountId);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("ecrUrl", ecrUrl)
        .add("accessKey", accessKey)
        .add("region", region)
        .add("accountId", accountId)
        .toString();
  }

  public static final class EcrConfigBuilder {
    private String ecrUrl;
    private String accessKey;
    private char[] secretKey;
    private String region;
    private String accountId;

    private EcrConfigBuilder() {}

    public static EcrConfigBuilder anEcrConfig() {
      return new EcrConfigBuilder();
    }

    public EcrConfigBuilder withEcrUrl(String ecrUrl) {
      this.ecrUrl = ecrUrl;
      return this;
    }

    public EcrConfigBuilder withAccessKey(String accessKey) {
      this.accessKey = accessKey;
      return this;
    }

    public EcrConfigBuilder withSecretKey(char[] secretKey) {
      this.secretKey = secretKey;
      return this;
    }

    public EcrConfigBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public EcrConfigBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public EcrConfigBuilder but() {
      return anEcrConfig()
          .withEcrUrl(ecrUrl)
          .withAccessKey(accessKey)
          .withSecretKey(secretKey)
          .withRegion(region)
          .withAccountId(accountId);
    }

    public EcrConfig build() {
      EcrConfig ecrConfig = new EcrConfig();
      ecrConfig.setEcrUrl(ecrUrl);
      ecrConfig.setAccessKey(accessKey);
      ecrConfig.setSecretKey(secretKey);
      ecrConfig.setRegion(region);
      ecrConfig.setAccountId(accountId);
      return ecrConfig;
    }
  }
}
