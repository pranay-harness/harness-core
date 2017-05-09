package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.api.LoadBalancerConfig;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@JsonTypeName("ELB")
public class ElasticLoadBalancerConfig extends LoadBalancerConfig {
  private Regions region;
  private String loadBalancerName;
  private String accessKey;
  private String secretKey;

  /**
   * Instantiates a new Elastic load balancer config.
   */
  public ElasticLoadBalancerConfig() {
    super(SettingVariableTypes.ELB.name());
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
  public String getSecretKey() {
    return secretKey;
  }

  /**
   * Setter for property 'secretKey'.
   *
   * @param secretKey Value to set for property 'secretKey'.
   */
  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
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
        && Objects.equals(this.accessKey, other.accessKey) && Objects.equals(this.secretKey, other.secretKey);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("region", region)
        .add("loadBalancerName", loadBalancerName)
        .add("accessKey", accessKey)
        .add("secretKey", secretKey)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Regions region;
    private String loadBalancerName;
    private String accessKey;
    private String secretKey;

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
      return anElasticLoadBalancerConfig()
          .withRegion(region)
          .withLoadBalancerName(loadBalancerName)
          .withAccessKey(accessKey)
          .withSecretKey(secretKey);
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
      return elasticLoadBalancerConfig;
    }
  }
}
