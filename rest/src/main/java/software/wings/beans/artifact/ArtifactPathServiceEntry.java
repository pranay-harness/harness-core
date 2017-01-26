package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;

import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
@Embedded
public class ArtifactPathServiceEntry {
  @NotEmpty private String artifactPathRegex;

  @Transient @SchemaIgnore private List<Service> services;

  private List<String> serviceIds = new ArrayList<>();

  /**
   * Gets artifact path regex.
   *
   * @return the artifact path regex
   */
  public String getArtifactPathRegex() {
    return artifactPathRegex;
  }

  /**
   * Sets artifact path regex.
   *
   * @param artifactPathRegex the artifact path regex
   */
  public void setArtifactPathRegex(String artifactPathRegex) {
    this.artifactPathRegex = artifactPathRegex;
  }

  /**
   * Gets services.
   *
   * @return the services
   */
  @SchemaIgnore
  public List<Service> getServices() {
    return services;
  }

  /**
   * Sets services.
   *
   * @param services the services
   */
  public void setServices(List<Service> services) {
    this.services = services;
  }

  /**
   * Gets service ids.
   *
   * @return the service ids
   */
  public List<String> getServiceIds() {
    return serviceIds;
  }

  /**
   * Sets service ids.
   *
   * @param serviceIds the service ids
   */
  public void setServiceIds(List<String> serviceIds) {
    this.serviceIds = serviceIds;
  }

  @Override
  public int hashCode() {
    return Objects.hash(artifactPathRegex, services, serviceIds);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ArtifactPathServiceEntry other = (ArtifactPathServiceEntry) obj;
    return Objects.equals(this.artifactPathRegex, other.artifactPathRegex)
        && Objects.equals(this.services, other.services) && Objects.equals(this.serviceIds, other.serviceIds);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("artifactPathRegex", artifactPathRegex)
        .add("services", services)
        .add("serviceIds", serviceIds)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String artifactPathRegex;
    private List<String> serviceIds = new ArrayList<>();

    private Builder() {}

    /**
     * An artifact path service entry builder.
     *
     * @return the builder
     */
    public static Builder anArtifactPathServiceEntry() {
      return new Builder();
    }

    /**
     * With artifact path regex builder.
     *
     * @param artifactPathRegex the artifact path regex
     * @return the builder
     */
    public Builder withArtifactPathRegex(String artifactPathRegex) {
      this.artifactPathRegex = artifactPathRegex;
      return this;
    }

    /**
     * With service ids builder.
     *
     * @param serviceIds the service ids
     * @return the builder
     */
    public Builder withServiceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactPathServiceEntry().withArtifactPathRegex(artifactPathRegex).withServiceIds(serviceIds);
    }

    /**
     * Build artifact path service entry.
     *
     * @return the artifact path service entry
     */
    public ArtifactPathServiceEntry build() {
      ArtifactPathServiceEntry artifactPathServiceEntry = new ArtifactPathServiceEntry();
      artifactPathServiceEntry.setArtifactPathRegex(artifactPathRegex);
      artifactPathServiceEntry.setServiceIds(serviceIds);
      return artifactPathServiceEntry;
    }
  }
}
