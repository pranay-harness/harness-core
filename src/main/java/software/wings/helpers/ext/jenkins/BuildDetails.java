package software.wings.helpers.ext.jenkins;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class BuildDetails {
  private int number;
  private String revision;

  public int getNumber() {
    return number;
  }

  public void setNumber(int number) {
    this.number = number;
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BuildDetails that = (BuildDetails) o;
    return number == that.number && Objects.equal(revision, that.revision);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(number, revision);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("number", number).add("revision", revision).toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private int number;
    private String revision;

    private Builder() {}

    /**
     * A build details.
     *
     * @return the builder
     */
    public static Builder aBuildDetails() {
      return new Builder();
    }

    /**
     * With number.
     *
     * @param number the number
     * @return the builder
     */
    public Builder withNumber(int number) {
      this.number = number;
      return this;
    }

    /**
     * With revision.
     *
     * @param revision the revision
     * @return the builder
     */
    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aBuildDetails().withNumber(number).withRevision(revision);
    }

    /**
     * Builds the.
     *
     * @return the builds the details
     */
    public BuildDetails build() {
      BuildDetails buildDetails = new BuildDetails();
      buildDetails.setNumber(number);
      buildDetails.setRevision(revision);
      return buildDetails;
    }
  }
}
