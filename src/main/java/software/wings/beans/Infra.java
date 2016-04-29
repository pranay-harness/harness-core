package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.Objects;

/**
 * Created by anubhaw on 4/1/16.
 */
@Entity(value = "infras", noClassnameStored = true)
public class Infra extends Base {
  public static enum InfraType { STATIC, AWS, AZURE, CONTAINER }
  private InfraType infraType;

  private String envId;

  public InfraType getInfraType() {
    return infraType;
  }

  public void setInfraType(InfraType infraType) {
    this.infraType = infraType;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(infraType, envId);
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
    final Infra other = (Infra) obj;
    return Objects.equals(this.infraType, other.infraType) && Objects.equals(this.envId, other.envId);
  }

  public static final class InfraBuilder {
    private InfraType infraType;
    private String envId;
    private String uuid;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private InfraBuilder() {}

    public static InfraBuilder anInfra() {
      return new InfraBuilder();
    }

    public InfraBuilder withInfraType(InfraType infraType) {
      this.infraType = infraType;
      return this;
    }

    public InfraBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public InfraBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public InfraBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public InfraBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public InfraBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public InfraBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public InfraBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public InfraBuilder but() {
      return anInfra()
          .withInfraType(infraType)
          .withEnvId(envId)
          .withUuid(uuid)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public Infra build() {
      Infra infra = new Infra();
      infra.setInfraType(infraType);
      infra.setEnvId(envId);
      infra.setUuid(uuid);
      infra.setCreatedBy(createdBy);
      infra.setCreatedAt(createdAt);
      infra.setLastUpdatedBy(lastUpdatedBy);
      infra.setLastUpdatedAt(lastUpdatedAt);
      infra.setActive(active);
      return infra;
    }
  }
}
