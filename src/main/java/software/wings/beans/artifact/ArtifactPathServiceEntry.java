package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;
import software.wings.beans.Service;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
@Embedded
public class ArtifactPathServiceEntry {
  @NotEmpty private String artifactPathRegex;

  @Reference(idOnly = true, ignoreMissing = true, lazy = true) private List<Service> services;

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

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ArtifactPathServiceEntry that = (ArtifactPathServiceEntry) obj;
    return Objects.equal(artifactPathRegex, that.artifactPathRegex) && Objects.equal(services, that.services);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(artifactPathRegex, services);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("serviceIds", services)
        .add("artifactPathRegex", artifactPathRegex)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String artifactPathRegex;
    private List<Service> services;

    private Builder() {}

    /**
     * An artifact path service entry.
     *
     * @return the builder
     */
    public static Builder anArtifactPathServiceEntry() {
      return new Builder();
    }

    /**
     * With artifact path regex.
     *
     * @param artifactPathRegex the artifact path regex
     * @return the builder
     */
    public Builder withArtifactPathRegex(String artifactPathRegex) {
      this.artifactPathRegex = artifactPathRegex;
      return this;
    }

    /**
     * With services.
     *
     * @param services the services
     * @return the builder
     */
    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    /**
     * Builds the.
     *
     * @return A new ArtifactPathServiceEntry object.
     */
    public ArtifactPathServiceEntry build() {
      ArtifactPathServiceEntry artifactPathServiceEntry = new ArtifactPathServiceEntry();
      artifactPathServiceEntry.setArtifactPathRegex(artifactPathRegex);
      artifactPathServiceEntry.setServices(services);
      return artifactPathServiceEntry;
    }
  }
}
