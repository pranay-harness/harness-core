package software.wings.helpers.ext.jenkins;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Has all the job related info required by the UI to show jenkins job tree
 * Created by rtummala on 7/19/17.
 */
public class JobDetails {
  private String jobName;
  private String url;
  private boolean isFolder;

  // Added for kryo serializer
  public JobDetails() {}

  public JobDetails(String jobName, boolean isFolder) {
    this.jobName = jobName;
    this.isFolder = isFolder;
  }

  public JobDetails(String jobName, String url, boolean isFolder) {
    this.jobName = jobName;
    this.url = url;
    this.isFolder = isFolder;
  }

  public String getJobName() {
    return jobName;
  }

  public boolean isFolder() {
    return isFolder;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobName, isFolder);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final JobDetails other = (JobDetails) obj;
    return Objects.equals(this.jobName, other.jobName) && this.isFolder == other.isFolder;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("jobName", jobName).add("isFolder", isFolder).toString();
  }

  public String getUrl() {
    return url;
  }
}
