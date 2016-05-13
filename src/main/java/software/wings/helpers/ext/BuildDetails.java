package software.wings.helpers.ext;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

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

  @Override
  public int hashCode() {
    return Objects.hashCode(number, revision);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("number", number).add("revision", revision).toString();
  }

  public static final class Builder {
    private int number;
    private String revision;

    private Builder() {}

    public static Builder aBuildDetails() {
      return new Builder();
    }

    public Builder withNumber(int number) {
      this.number = number;
      return this;
    }

    public Builder withRevision(String revision) {
      this.revision = revision;
      return this;
    }

    public Builder but() {
      return aBuildDetails().withNumber(number).withRevision(revision);
    }

    public BuildDetails build() {
      BuildDetails buildDetails = new BuildDetails();
      buildDetails.setNumber(number);
      buildDetails.setRevision(revision);
      return buildDetails;
    }
  }
}
