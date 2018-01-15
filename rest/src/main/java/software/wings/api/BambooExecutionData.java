package software.wings.api;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toMap;
import static software.wings.api.ExecutionDataValue.Builder.anExecutionDataValue;

import org.apache.commons.collections.CollectionUtils;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;
import software.wings.sm.states.FilePathAssertionEntry;
import software.wings.sm.states.ParameterEntry;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 * Created by sgurubelli on 8/29/17.
 */
public class BambooExecutionData extends StateExecutionData implements NotifyResponseData {
  private String projectName;
  private String planName;
  private String buildStatus;
  private String buildUrl;
  private String buildNumber;
  private String planUrl;
  private List<ParameterEntry> parameters;
  private List<FilePathAssertionEntry> filePathAssertionEntries;

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getPlanName() {
    return planName;
  }

  public void setPlanName(String planName) {
    this.planName = planName;
  }

  public String getBuildStatus() {
    return buildStatus;
  }

  public void setBuildStatus(String buildStatus) {
    this.buildStatus = buildStatus;
  }

  public String getBuildUrl() {
    return buildUrl;
  }

  public String getBuildNumber() {
    return buildNumber;
  }

  public void setBuildNumber(String buildNumber) {
    this.buildNumber = buildNumber;
  }

  public void setBuildUrl(String buildUrl) {
    this.buildUrl = buildUrl;
  }

  public String getPlanUrl() {
    return planUrl;
  }

  public void setPlanUrl(String planUrl) {
    this.planUrl = planUrl;
  }

  public List<ParameterEntry> getParameters() {
    return parameters;
  }

  public void setParameters(List<ParameterEntry> parameters) {
    this.parameters = parameters;
  }

  /**
   * Getter for property 'filePathAssertionEntries'.
   *
   * @return Value for property 'filePathAssertionEntries'.
   */
  public List<FilePathAssertionEntry> getFilePathAssertionEntries() {
    return filePathAssertionEntries;
  }

  /**
   * Setter for property 'filePathAssertionEntries'.
   *
   * @param filePathAssertionEntries Value to set for property 'filePathAssertionEntries'.
   */
  public void setFilePathAssertionEntries(List<FilePathAssertionEntry> filePathAssertionEntries) {
    this.filePathAssertionEntries = filePathAssertionEntries;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionSummary() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "projectName",
        anExecutionDataValue().withValue(projectName).withDisplayName("Project Name").build());
    putNotNull(
        executionDetails, "planName", anExecutionDataValue().withValue(planName).withDisplayName("Plan Name").build());
    if (CollectionUtils.isNotEmpty(parameters)) {
      Map<String, String> jobParameterMap = isEmpty(parameters)
          ? Collections.emptyMap()
          : parameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));
      putNotNull(executionDetails, "parameters",
          anExecutionDataValue().withValue(String.valueOf(jobParameterMap)).withDisplayName("Parameters").build());
    }
    if (CollectionUtils.isNotEmpty(filePathAssertionEntries)) {
      Map<String, String> filePathAsssertionMap = isEmpty(parameters)
          ? Collections.emptyMap()
          : parameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));
      putNotNull(executionDetails, "fileAssertionData",
          anExecutionDataValue()
              .withValue(String.valueOf(filePathAsssertionMap))
              .withDisplayName("Assertion Data")
              .build());
    }
    putNotNull(executionDetails, "buildNumber",
        anExecutionDataValue().withValue(buildNumber).withDisplayName("Build Number").build());
    putNotNull(executionDetails, "buildStatus",
        anExecutionDataValue().withValue(buildStatus).withDisplayName("Build Status").build());
    putNotNull(
        executionDetails, "buildUrl", anExecutionDataValue().withValue(buildUrl).withDisplayName("Build URL").build());
    return executionDetails;
  }

  @Override
  public Map<String, ExecutionDataValue> getExecutionDetails() {
    Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
    putNotNull(executionDetails, "projectName",
        anExecutionDataValue().withValue(projectName).withDisplayName("Project Name").build());
    putNotNull(
        executionDetails, "planName", anExecutionDataValue().withValue(planName).withDisplayName("Plan Name").build());
    if (CollectionUtils.isNotEmpty(parameters)) {
      Map<String, String> jobParameterMap = isEmpty(parameters)
          ? Collections.emptyMap()
          : parameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));
      putNotNull(executionDetails, "parameters",
          anExecutionDataValue().withValue(String.valueOf(jobParameterMap)).withDisplayName("Parameters").build());
    }

    if (CollectionUtils.isNotEmpty(filePathAssertionEntries)) {
      Map<String, String> filePathAsssertionMap = isEmpty(parameters)
          ? Collections.emptyMap()
          : parameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));
      putNotNull(executionDetails, "fileAssertionData",
          anExecutionDataValue()
              .withValue(String.valueOf(filePathAsssertionMap))
              .withDisplayName("Assertion Data")
              .build());
    }

    putNotNull(executionDetails, "buildNumber",
        anExecutionDataValue().withValue(buildNumber).withDisplayName("Build Number").build());
    putNotNull(executionDetails, "buildStatus",
        anExecutionDataValue().withValue(buildStatus).withDisplayName("Build Status").build());
    putNotNull(
        executionDetails, "buildUrl", anExecutionDataValue().withValue(buildUrl).withDisplayName("Build URL").build());

    return executionDetails;
  }
  public static final class Builder {
    private String projectName;
    private String planName;
    private String planUrl;
    private String buildUrl;
    private String buildStatus;
    private String buildNumber;
    private List<ParameterEntry> parameters;
    private List<FilePathAssertionEntry> filePathAssertionMap;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    public static Builder aBambooExecutionData() {
      return new Builder();
    }

    public Builder withProjectName(String projectName) {
      this.projectName = projectName;
      return this;
    }

    public Builder withPlanName(String planName) {
      this.planName = planName;
      return this;
    }

    public Builder withBuildUrl(String buildUrl) {
      this.buildUrl = buildUrl;
      return this;
    }

    public Builder withPlanUrl(String planUrl) {
      this.planUrl = planUrl;
      return this;
    }

    public Builder withBuildStatus(String buildStatus) {
      this.buildStatus = buildStatus;
      return this;
    }

    public Builder withBuildNumber(String buildStatus) {
      this.buildStatus = buildStatus;
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

    public Builder withParameters(List<ParameterEntry> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder withFilePathAssertionMap(List<FilePathAssertionEntry> filePathAssertionMap) {
      this.filePathAssertionMap = filePathAssertionMap;
      return this;
    }

    public Builder but() {
      return aBambooExecutionData()
          .withProjectName(projectName)
          .withPlanName(planName)
          .withPlanUrl(planUrl)
          .withBuildStatus(buildStatus)
          .withBuildUrl(buildUrl)
          .withParameters(parameters)
          .withFilePathAssertionMap(filePathAssertionMap)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    public BambooExecutionData build() {
      BambooExecutionData bambooExecutionData = new BambooExecutionData();
      bambooExecutionData.setProjectName(projectName);
      bambooExecutionData.setPlanName(planName);
      bambooExecutionData.setPlanUrl(planUrl);
      bambooExecutionData.setBuildStatus(buildStatus);
      bambooExecutionData.setBuildUrl(buildUrl);
      bambooExecutionData.setParameters(parameters);
      bambooExecutionData.setFilePathAssertionEntries(filePathAssertionMap);
      bambooExecutionData.setStatus(status);
      bambooExecutionData.setErrorMsg(errorMsg);
      return bambooExecutionData;
    }
  }
}
