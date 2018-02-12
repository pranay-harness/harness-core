package software.wings.helpers.ext.jenkins;

import com.google.common.base.MoreObjects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Has all the job related info required by the UI to show jenkins job tree
 * Created by rtummala on 7/19/17.
 */
public class JobDetails {
  private String jobName;
  private String url;
  private boolean isFolder;
  private List<JobParameter> parameters = new ArrayList<>();

  public JobDetails(String jobName, String url, List<JobParameter> parameters) {
    this.jobName = jobName;
    this.url = url;
    this.parameters = parameters;
  }

  public List<JobParameter> getParameters() {
    return parameters;
  }

  public void setParameters(List<JobParameter> parameters) {
    this.parameters = parameters;
  }

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

  public String getUrl() {
    return url;
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobName, isFolder);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("jobName", jobName).add("isFolder", isFolder).toString();
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

  public static class JobParameter {
    String name;
    List<String> values = new ArrayList<>();
    String defaultValue;
    String description;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public List<String> getValues() {
      return values;
    }

    public void setValues(List<String> values) {
      this.values = values;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }
}
