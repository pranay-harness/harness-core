package software.wings.api;

import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.JenkinsState.FilePathAssertionEntry;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/24/16.
 */
public class JenkinsExecutionData extends StateExecutionData {
  private String jobName;
  private String jobStatus;
  private String buildUrl;
  private List<FilePathAssertionEntry> filePathAssertionMap;
  private Map<String, String> jobParameters;

  /**
   * Getter for property 'buildUrl'.
   *
   * @return Value for property 'buildUrl'.
   */
  public String getBuildUrl() {
    return buildUrl;
  }

  /**
   * Setter for property 'buildUrl'.
   *
   * @param buildUrl Value to set for property 'buildUrl'.
   */
  public void setBuildUrl(String buildUrl) {
    this.buildUrl = buildUrl;
  }

  /**
   * Getter for property 'jobName'.
   *
   * @return Value for property 'jobName'.
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * Setter for property 'jobName'.
   *
   * @param jobName Value to set for property 'jobName'.
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  /**
   * Getter for property 'jobStatus'.
   *
   * @return Value for property 'jobStatus'.
   */
  public String getJobStatus() {
    return jobStatus;
  }

  /**
   * Setter for property 'jobStatus'.
   *
   * @param jobStatus Value to set for property 'jobStatus'.
   */
  public void setJobStatus(String jobStatus) {
    this.jobStatus = jobStatus;
  }

  /**
   * Getter for property 'filePathAssertionMap'.
   *
   * @return Value for property 'filePathAssertionMap'.
   */
  public List<FilePathAssertionEntry> getFilePathAssertionMap() {
    return filePathAssertionMap;
  }

  /**
   * Setter for property 'filePathAssertionMap'.
   *
   * @param filePathAssertionMap Value to set for property 'filePathAssertionMap'.
   */
  public void setFilePathAssertionMap(List<FilePathAssertionEntry> filePathAssertionMap) {
    this.filePathAssertionMap = filePathAssertionMap;
  }

  /**
   * Getter for property 'jobParameters'.
   *
   * @return Value for property 'jobParameters'.
   */
  public Map<String, String> getJobParameters() {
    return jobParameters;
  }

  /**
   * Setter for property 'jobParameters'.
   *
   * @param jobParameters Value to set for property 'jobParameters'.
   */
  public void setJobParameters(Map<String, String> jobParameters) {
    this.jobParameters = jobParameters;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
    putNotNull(
        executionDetails, "jobName", anExecutionDataValue().withValue(jobName).withDisplayName("Job Name").build());
    putNotNull(executionDetails, "build", anExecutionDataValue().withValue(buildUrl).withDisplayName("Build").build());
    putNotNull(executionDetails, "jobParameters",
        anExecutionDataValue().withValue(jobParameters).withDisplayName("Job Parameters").build());
    putNotNull(executionDetails, "jobStatus",
        anExecutionDataValue().withValue(jobStatus).withDisplayName("Job Status").build());
    putNotNull(executionDetails, "fileAssertionData",
        anExecutionDataValue().withValue(filePathAssertionMap).withDisplayName("Assertion Data").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(
        executionDetails, "jobName", anExecutionDataValue().withValue(jobName).withDisplayName("Job Name").build());
    putNotNull(executionDetails, "build", anExecutionDataValue().withValue(buildUrl).withDisplayName("Build").build());
    putNotNull(executionDetails, "jobParameters",
        anExecutionDataValue().withValue(jobParameters).withDisplayName("Job Parameters").build());
    putNotNull(executionDetails, "jobStatus",
        anExecutionDataValue().withValue(jobStatus).withDisplayName("Job Status").build());
    putNotNull(executionDetails, "fileAssertionData",
        anExecutionDataValue().withValue(filePathAssertionMap).withDisplayName("Assertion Data").build());
    return executionDetails;
  }

  public static final class Builder {
    private String jobName;
    private String jobStatus;
    private String buildUrl;
    private List<FilePathAssertionEntry> filePathAssertionMap;
    private ExecutionStatus status;
    private String errorMsg;
    private Map<String, String> jobParameters;

    private Builder() {}

    public static Builder aJenkinsExecutionData() {
      return new Builder();
    }

    public Builder withJobName(String jobName) {
      this.jobName = jobName;
      return this;
    }

    public Builder withJobStatus(String jobStatus) {
      this.jobStatus = jobStatus;
      return this;
    }

    public Builder withBuildUrl(String buildUrl) {
      this.buildUrl = buildUrl;
      return this;
    }

    public Builder withFilePathAssertionMap(List<FilePathAssertionEntry> filePathAssertionMap) {
      this.filePathAssertionMap = filePathAssertionMap;
      return this;
    }

    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    public Builder withJobParameters(Map<String, String> jobParameters) {
      this.jobParameters = jobParameters;
      return this;
    }

    public Builder but() {
      return aJenkinsExecutionData()
          .withJobName(jobName)
          .withJobStatus(jobStatus)
          .withBuildUrl(buildUrl)
          .withFilePathAssertionMap(filePathAssertionMap)
          .withStatus(status)
          .withErrorMsg(errorMsg)
          .withJobParameters(jobParameters);
    }

    public JenkinsExecutionData build() {
      JenkinsExecutionData jenkinsExecutionData = new JenkinsExecutionData();
      jenkinsExecutionData.setJobName(jobName);
      jenkinsExecutionData.setJobStatus(jobStatus);
      jenkinsExecutionData.setBuildUrl(buildUrl);
      jenkinsExecutionData.setFilePathAssertionMap(filePathAssertionMap);
      jenkinsExecutionData.setStatus(status);
      jenkinsExecutionData.setErrorMsg(errorMsg);
      jenkinsExecutionData.setJobParameters(jobParameters);
      return jenkinsExecutionData;
    }
  }
}
