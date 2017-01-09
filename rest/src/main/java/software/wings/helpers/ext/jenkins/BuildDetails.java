package software.wings.helpers.ext.jenkins;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class BuildDetails {
  private String number;
  private String revision;

  /**
   * Gets number.
   *
   * @return the number
   */
  public String getNumber() {
    return number;
  }

  /**
   * Sets number.
   *
   * @param number the number
   */
  public void setNumber(String number) {
    this.number = number;
  }

  /**
   * Gets revision.
   *
   * @return the revision
   */
  public String getRevision() {
    return revision;
  }

  /**
   * Sets revision.
   *
   * @param revision the revision
   */
  public void setRevision(String revision) {
    this.revision = revision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, revision);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final BuildDetails other = (BuildDetails) obj;
    return Objects.equals(this.number, other.number) && Objects.equals(this.revision, other.revision);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("number", number).add("revision", revision).toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String number;
    private String revision;

    private Builder() {}

    /**
     * A build details builder.
     *
     * @return the builder
     */
    public static Builder aBuildDetails() {
      return new Builder();
    }

    /**
     * With number builder.
     *
     * @param number the number
     * @return the builder
     */
    public Builder withNumber(String number) {
      this.number = number;
      return this;
    }

    /**
     * With revision builder.
     *
     * @param revision the revision
     * @return the builder
     */
    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBuildDetails().withNumber(number).withRevision(revision);
    }

    /**
     * Build build details.
     *
     * @return the build details
     */
    public BuildDetails build() {
      BuildDetails buildDetails = new BuildDetails();
      buildDetails.setNumber(number);
      buildDetails.setRevision(revision);
      return buildDetails;
    }
  }
}
