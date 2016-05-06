package software.wings.beans;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.Service.ServiceBuilder.aService;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.PostLoad;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.annotations.Reference;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/4/16.
 */
@Embedded
public class ArtifactPathServiceEntry {
  @NotEmpty private String artifactPathRegex;

  @Property(value = "services") private List<String> serviceIds;

  public String getArtifactPathRegex() {
    return artifactPathRegex;
  }

  public void setArtifactPathRegex(String artifactPathRegex) {
    this.artifactPathRegex = artifactPathRegex;
  }

  public List<String> getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(List<String> serviceIds) {
    this.serviceIds = serviceIds;
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
    return Objects.equal(artifactPathRegex, that.artifactPathRegex) && Objects.equal(serviceIds, that.serviceIds);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(artifactPathRegex, serviceIds);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("serviceIds", serviceIds)
        .add("artifactPathRegex", artifactPathRegex)
        .toString();
  }

  public static final class Builder {
    private String artifactPathRegex;
    private List<String> serviceIds;

    private Builder() {}

    public static Builder anArtifactPathServiceEntry() {
      return new Builder();
    }

    public Builder withArtifactPathRegex(String artifactPathRegex) {
      this.artifactPathRegex = artifactPathRegex;
      return this;
    }

    public Builder withServiceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    /**
     * @return A new ArtifactPathServiceEntry object.
     */
    public ArtifactPathServiceEntry build() {
      ArtifactPathServiceEntry artifactPathServiceEntry = new ArtifactPathServiceEntry();
      artifactPathServiceEntry.setArtifactPathRegex(artifactPathRegex);
      artifactPathServiceEntry.setServiceIds(serviceIds);
      return artifactPathServiceEntry;
    }
  }
}
