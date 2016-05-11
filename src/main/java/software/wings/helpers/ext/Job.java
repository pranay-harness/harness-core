package software.wings.helpers.ext;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/9/16.
 */
public class Job {
  private String displayName;
  private List<Build> builds;

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public List<Build> getBuilds() {
    return builds;
  }

  public void setBuilds(List<Build> builds) {
    this.builds = builds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Job job = (Job) o;
    return Objects.equal(displayName, job.displayName) && Objects.equal(builds, job.builds);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(displayName, builds);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("displayName", displayName).add("builds", builds).toString();
  }

  public static final class Builder {
    private String displayName;
    private List<Build> builds;

    private Builder() {}

    public static Builder aJob() {
      return new Builder();
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withBuilds(List<Build> builds) {
      this.builds = builds;
      return this;
    }

    public Builder but() {
      return aJob().withDisplayName(displayName).withBuilds(builds);
    }

    public Job build() {
      Job job = new Job();
      job.setDisplayName(displayName);
      job.setBuilds(builds);
      return job;
    }
  }
}
