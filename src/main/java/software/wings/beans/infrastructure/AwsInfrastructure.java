package software.wings.beans.infrastructure;

import com.google.common.base.MoreObjects;

import software.wings.beans.User;

import java.util.Objects;

/**
 * Created by anubhaw on 9/13/16.
 */
public class AwsInfrastructure extends Infrastructure {
  private String accessKeyId;
  private String secretAccessKey;
  private String serviceUrl;

  /**
   * Instantiates a new Infrastructure.
   */
  public AwsInfrastructure() {
    super(InfrastructureType.AWS);
  }

  /**
   * Gets access key id.
   *
   * @return the access key id
   */
  public String getAccessKeyId() {
    return accessKeyId;
  }

  /**
   * Sets access key id.
   *
   * @param accessKeyId the access key id
   */
  public void setAccessKeyId(String accessKeyId) {
    this.accessKeyId = accessKeyId;
  }

  /**
   * Gets secret access key.
   *
   * @return the secret access key
   */
  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  /**
   * Sets secret access key.
   *
   * @param secretAccessKey the secret access key
   */
  public void setSecretAccessKey(String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
  }

  /**
   * Gets service url.
   *
   * @return the service url
   */
  public String getServiceUrl() {
    return serviceUrl;
  }

  /**
   * Sets service url.
   *
   * @param serviceUrl the service url
   */
  public void setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(accessKeyId, secretAccessKey, serviceUrl);
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
    final AwsInfrastructure other = (AwsInfrastructure) obj;
    return Objects.equals(this.accessKeyId, other.accessKeyId)
        && Objects.equals(this.secretAccessKey, other.secretAccessKey)
        && Objects.equals(this.serviceUrl, other.serviceUrl);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("accessKeyId", accessKeyId)
        .add("secretAccessKey", secretAccessKey)
        .add("serviceUrl", serviceUrl)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String accessKeyId;
    private String secretAccessKey;
    private String serviceUrl;
    private String name;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An aws infrastructure builder.
     *
     * @return the builder
     */
    public static Builder anAwsInfrastructure() {
      return new Builder();
    }

    /**
     * With access key id builder.
     *
     * @param accessKeyId the access key id
     * @return the builder
     */
    public Builder withAccessKeyId(String accessKeyId) {
      this.accessKeyId = accessKeyId;
      return this;
    }

    /**
     * With secret access key builder.
     *
     * @param secretAccessKey the secret access key
     * @return the builder
     */
    public Builder withSecretAccessKey(String secretAccessKey) {
      this.secretAccessKey = secretAccessKey;
      return this;
    }

    /**
     * With service url builder.
     *
     * @param serviceUrl the service url
     * @return the builder
     */
    public Builder withServiceUrl(String serviceUrl) {
      this.serviceUrl = serviceUrl;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAwsInfrastructure()
          .withAccessKeyId(accessKeyId)
          .withSecretAccessKey(secretAccessKey)
          .withServiceUrl(serviceUrl)
          .withName(name)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build aws infrastructure.
     *
     * @return the aws infrastructure
     */
    public AwsInfrastructure build() {
      AwsInfrastructure awsInfrastructure = new AwsInfrastructure();
      awsInfrastructure.setAccessKeyId(accessKeyId);
      awsInfrastructure.setSecretAccessKey(secretAccessKey);
      awsInfrastructure.setServiceUrl(serviceUrl);
      awsInfrastructure.setName(name);
      awsInfrastructure.setUuid(uuid);
      awsInfrastructure.setAppId(appId);
      awsInfrastructure.setCreatedBy(createdBy);
      awsInfrastructure.setCreatedAt(createdAt);
      awsInfrastructure.setLastUpdatedBy(lastUpdatedBy);
      awsInfrastructure.setLastUpdatedAt(lastUpdatedAt);
      awsInfrastructure.setActive(active);
      return awsInfrastructure;
    }
  }
}
