package software.wings.sm;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by sgurubelli on 7/26/17.
 */
public class InfraMappingSummary {
  private String infraMappingId;
  private String computeProviderType;
  private String infraMappingType;
  private String deploymentType;
  private String computeProviderName;
  private String displayName;

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public String getComputeProviderType() {
    return computeProviderType;
  }

  public void setComputeProviderType(String computeProviderType) {
    this.computeProviderType = computeProviderType;
  }

  public String getInfraMappingType() {
    return infraMappingType;
  }

  public void setInfraMappingType(String infraMappingType) {
    this.infraMappingType = infraMappingType;
  }

  public String getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getComputeProviderName() {
    return computeProviderName;
  }

  public void setComputeProviderName(String computeProviderName) {
    this.computeProviderName = computeProviderName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InfraMappingSummary that = (InfraMappingSummary) o;
    return Objects.equals(infraMappingId, that.infraMappingId)
        && Objects.equals(computeProviderType, that.computeProviderType)
        && Objects.equals(infraMappingType, that.infraMappingType)
        && Objects.equals(deploymentType, that.deploymentType)
        && Objects.equals(computeProviderName, that.computeProviderName)
        && Objects.equals(displayName, that.displayName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        infraMappingId, computeProviderType, infraMappingType, deploymentType, computeProviderName, displayName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("infraMappingId", infraMappingId)
        .add("computeProviderType", computeProviderType)
        .add("infraMappingType", infraMappingType)
        .add("deploymentType", deploymentType)
        .add("computeProviderName", computeProviderName)
        .add("displayName", displayName)
        .toString();
  }

  public static final class Builder {
    private String infraMappingId;
    private String computeProviderType;
    private String infraMappingType;
    private String deploymentType;
    private String computeProviderName;
    private String displayName;

    private Builder() {}

    /**
     * An InframappingSummary builder.
     * @return
     */
    public static Builder anInfraMappingSummary() {
      return new Builder();
    }

    /**
     * With inframappingId builder
     */
    public Builder withInframappingId(String infraMappingId) {
      this.infraMappingId = infraMappingId;
      return this;
    }

    /**
     * With ComputerProviderType builder
     */
    public Builder withComputerProviderType(String computeProviderType) {
      this.computeProviderType = computeProviderType;
      return this;
    }

    /**
     * With InfraMappingType builder
     */
    public Builder withInfraMappingType(String infraMappingType) {
      this.infraMappingType = infraMappingType;
      return this;
    }

    /**
     * With DeploymentType builder
     */
    public Builder withDeploymentType(String deploymentType) {
      this.deploymentType = deploymentType;
      return this;
    }

    /**
     * With ComputerProviderName builder
     * @param computeProviderName
     * @return
     */
    public Builder withComputerProviderName(String computeProviderName) {
      this.computeProviderName = computeProviderName;
      return this;
    }

    /**
     * With DisplayName builder
     * @param displayName
     * @return
     */
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    /**
     * But Builder
     * @return
     */
    public Builder but() {
      return anInfraMappingSummary()
          .withInframappingId(infraMappingId)
          .withComputerProviderName(computeProviderName)
          .withComputerProviderType(computeProviderType)
          .withDeploymentType(deploymentType)
          .withInfraMappingType(infraMappingType)
          .withDisplayName(displayName);
    }

    public InfraMappingSummary build() {
      InfraMappingSummary infraMappingSummary = new InfraMappingSummary();
      infraMappingSummary.setInfraMappingId(infraMappingId);
      infraMappingSummary.setInfraMappingType(infraMappingType);
      infraMappingSummary.setComputeProviderName(computeProviderName);
      infraMappingSummary.setComputeProviderType(computeProviderType);
      infraMappingSummary.setDeploymentType(deploymentType);
      infraMappingSummary.setDisplayName(displayName);
      return infraMappingSummary;
    }
  }
}
