package software.wings.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Reference;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
@Embedded
public class ArtifactPathServiceEntry {
  @NotEmpty private String artifactPathRegex;

  @Reference(idOnly = true, ignoreMissing = true, lazy = true) private List<Service> services;

  public String getArtifactPathRegex() {
    return artifactPathRegex;
  }

  public void setArtifactPathRegex(String artifactPathRegex) {
    this.artifactPathRegex = artifactPathRegex;
  }

  public List<Service> getServices() {
    return services;
  }

  public void setServices(List<Service> services) {
    this.services = services;
  }

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

  @Override
  public int hashCode() {
    return Objects.hashCode(artifactPathRegex, services);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("serviceIds", services)
        .add("artifactPathRegex", artifactPathRegex)
        .toString();
  }

  public static final class Builder {
    private String artifactPathRegex;
    private List<Service> services;

    private Builder() {}

    public static Builder anArtifactPathServiceEntry() {
      return new Builder();
    }

    public Builder withArtifactPathRegex(String artifactPathRegex) {
      this.artifactPathRegex = artifactPathRegex;
      return this;
    }

    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    /**
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
