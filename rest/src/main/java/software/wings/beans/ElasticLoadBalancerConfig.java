package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.api.LoadBalancerConfig;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@JsonTypeName("ELB")
public class ElasticLoadBalancerConfig extends LoadBalancerConfig implements Encryptable {
  @Attributes(title = "Region", required = true)
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private Regions region;

  @NotEmpty @Attributes(title = "Elastic Load Balancer Name", required = true) private String loadBalancerName;

  @Attributes(title = "AWS account access key", required = true) @NotEmpty private String accessKey;

  @Attributes(title = "AWS account secret key", required = true) @NotEmpty @Encrypted private char[] secretKey;

  @SchemaIgnore @NotEmpty private String accountId;

  @Attributes(title = "Encrypted Fields", required = true)
  private final static List<String> encryptedFields = Arrays.asList("secretKey");

  /**
   * Instantiates a new Elastic load balancer config.
   */
  public ElasticLoadBalancerConfig() {
    super(SettingVariableTypes.ELB.name());
  }

  /**
   * Gets the list of fields that are encrypted for use in the UI
   * @return List of field names
   */
  @JsonIgnore
  public List<String> getEncryptedFields() {
    return encryptedFields;
  }

  /**
   * Getter for property 'region'.
   *
   * @return Value for property 'region'.
   */
  public Regions getRegion() {
    return region;
  }

  /**
   * Setter for property 'region'.
   *
   * @param region Value to set for property 'region'.
   */
  public void setRegion(Regions region) {
    this.region = region;
  }

  /**
   * Getter for property 'loadBalancerName'.
   *
   * @return Value for property 'loadBalancerName'.
   */
  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  /**
   * Setter for property 'loadBalancerName'.
   *
   * @param loadBalancerName Value to set for property 'loadBalancerName'.
   */
  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  /**
   * Getter for property 'accessKey'.
   *
   * @return Value for property 'accessKey'.
   */
  public String getAccessKey() {
    return accessKey;
  }

  /**
   * Setter for property 'accessKey'.
   *
   * @param accessKey Value to set for property 'accessKey'.
   */
  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  /**
   * Getter for property 'secretKey'.
   *
   * @return Value for property 'secretKey'.
   */
  public char[] getSecretKey() {
    return secretKey;
  }

  /**
   * Setter for property 'secretKey'.
   *
   * @param secretKey Value to set for property 'secretKey'.
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

  @Override
  public int hashCode() {
    return Objects.hash(region, loadBalancerName, accessKey, secretKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ElasticLoadBalancerConfig other = (ElasticLoadBalancerConfig) obj;
    return Objects.equals(this.region, other.region) && Objects.equals(this.loadBalancerName, other.loadBalancerName)
        && Objects.equals(this.accessKey, other.accessKey) && Arrays.equals(this.secretKey, other.secretKey);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("region", region)
        .add("loadBalancerName", loadBalancerName)
        .add("accessKey", accessKey)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Regions region;
    private String loadBalancerName;
    private String accessKey;
    private char[] secretKey;
    private String accountId;

    private Builder() {}

    /**
     * An elastic load balancer config builder.
     *
     * @return the builder
     */
    public static Builder anElasticLoadBalancerConfig() {
      return new Builder();
    }

    /**
     * With region builder.
     *
     * @param region the region
     * @return the builder
     */
    public Builder withRegion(Regions region) {
      this.region = region;
      return this;
    }

    /**
     * With load balancer name builder.
     *
     * @param loadBalancerName the load balancer name
     * @return the builder
     */
    public Builder withLoadBalancerName(String loadBalancerName) {
      this.loadBalancerName = loadBalancerName;
      return this;
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
      return anElasticLoadBalancerConfig()
          .withRegion(region)
          .withLoadBalancerName(loadBalancerName)
          .withAccessKey(accessKey)
          .withSecretKey(secretKey)
          .withAccountId(accountId);
    }

    /**
     * Build elastic load balancer config.
     *
     * @return the elastic load balancer config
     */
    public ElasticLoadBalancerConfig build() {
      ElasticLoadBalancerConfig elasticLoadBalancerConfig = new ElasticLoadBalancerConfig();
      elasticLoadBalancerConfig.setRegion(region);
      elasticLoadBalancerConfig.setLoadBalancerName(loadBalancerName);
      elasticLoadBalancerConfig.setAccessKey(accessKey);
      elasticLoadBalancerConfig.setSecretKey(secretKey);
      elasticLoadBalancerConfig.setAccountId(accountId);
      return elasticLoadBalancerConfig;
    }
  }
}
