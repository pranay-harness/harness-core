package software.wings.sm.states;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections.MapUtils.isEmpty;
import static software.wings.api.JenkinsExecutionData.Builder.aJenkinsExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.QueueReference;
import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import software.wings.api.InstanceElement;
import software.wings.api.JenkinsExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.JenkinsConfig;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.impl.JenkinsSettingProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.JenkinsState.FilePathAssertionEntry.Status;
import software.wings.stencils.EnumData;
import software.wings.utils.Misc;
import software.wings.utils.XmlUtils;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by peeyushaggarwal on 10/21/16.
 */
public class JenkinsState extends State {
  @Transient private final Logger logger = LoggerFactory.getLogger(JenkinsState.class);

  @EnumData(enumDataProvider = JenkinsSettingProvider.class)
  @Attributes(title = "Jenkins Server")
  private String jenkinsConfigId;

  @Attributes(title = "Job Name") private String jobName;

  @Attributes(title = "Job Parameters") private List<ParameterEntry> jobParameters = Lists.newArrayList();

  @Attributes(title = "Artifacts/Files Paths")
  private List<FilePathAssertionEntry> filePathsForAssertion = Lists.newArrayList();

  @Transient @Inject private JenkinsFactory jenkinsFactory;
  @Transient @Inject private SettingsService settingsService;
  @Transient @Inject private ExecutorService executorService;
  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private WaitNotifyEngine waitNotifyEngine;

  public JenkinsState(String name) {
    super(name, StateType.JENKINS.name());
  }

  /**
   * Getter for property 'jenkinsConfigId'.
   *
   * @return Value for property 'jenkinsConfigId'.
   */
  public String getJenkinsConfigId() {
    return jenkinsConfigId;
  }

  /**
   * Setter for property 'jenkinsConfigId'.
   *
   * @param jenkinsConfigId Value to set for property 'jenkinsConfigId'.
   */
  public void setJenkinsConfigId(String jenkinsConfigId) {
    this.jenkinsConfigId = jenkinsConfigId;
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
   * Getter for property 'jobParameters'.
   *
   * @return Value for property 'jobParameters'.
   */
  public List<ParameterEntry> getJobParameters() {
    return jobParameters;
  }

  /**
   * Setter for property 'jobParameters'.
   *
   * @param jobParameters Value to set for property 'jobParameters'.
   */
  public void setJobParameters(List<ParameterEntry> jobParameters) {
    this.jobParameters = jobParameters;
  }

  /**
   * Getter for property 'filePathsForAssertion'.
   *
   * @return Value for property 'filePathsForAssertion'.
   */
  public List<FilePathAssertionEntry> getFilePathsForAssertion() {
    return filePathsForAssertion;
  }

  /**
   * Setter for property 'filePathsForAssertion'.
   *
   * @param filePathsForAssertion Value to set for property 'filePathsForAssertion'.
   */
  public void setFilePathsForAssertion(List<FilePathAssertionEntry> filePathsForAssertion) {
    this.filePathsForAssertion = filePathsForAssertion;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    JenkinsConfig jenkinsConfig = (JenkinsConfig) settingsService.get(GLOBAL_APP_ID, jenkinsConfigId).getValue();

    String evaluatedJobName;
    try {
      evaluatedJobName = context.renderExpression(jobName);
    } catch (Exception e) {
      evaluatedJobName = jobName;
    }

    Map<String, String> jobParameterMap = CollectionUtils.isEmpty(jobParameters)
        ? Collections.emptyMap()
        : jobParameters.stream().collect(toMap(ParameterEntry::getKey, ParameterEntry::getValue));
    final String finalJobName = evaluatedJobName;

    Map<String, String> evaluatedParameters = Maps.newHashMap(jobParameterMap);
    evaluatedParameters.forEach((key, value) -> {
      String evaluatedValue;
      try {
        evaluatedValue = context.renderExpression(value);
      } catch (Exception e) {
        evaluatedValue = value;
      }
      evaluatedParameters.put(key, evaluatedValue);
    });

    Map<String, String> evaluatedFilePathsForAssertion = Maps.newHashMap();
    if (isNotEmpty(filePathsForAssertion)) {
      filePathsForAssertion.forEach(filePathAssertionMap -> {
        String evaluatedKey;
        try {
          evaluatedKey = context.renderExpression(filePathAssertionMap.getFilePath());
        } catch (Exception e) {
          evaluatedKey = filePathAssertionMap.getFilePath();
        }
        evaluatedFilePathsForAssertion.put(evaluatedKey, filePathAssertionMap.getAssertion());
      });
    }

    JenkinsExecutionData jenkinsExecutionData =
        aJenkinsExecutionData().withJobName(finalJobName).withJobParameters(evaluatedParameters).build();

    final String finalActivityId = activityId;
    executorService.execute(() -> {
      JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();
      jenkinsExecutionResponse.setActivityId(activityId);
      ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
      String errorMessage = null;
      try {
        Jenkins jenkins = jenkinsFactory.create(
            jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());

        QueueReference queueItem = jenkins.trigger(finalJobName, evaluatedParameters);

        Build jenkinsBuild;
        while ((jenkinsBuild = jenkins.getBuild(queueItem)) == null) {
          Misc.quietSleep(1000);
        }
        BuildWithDetails jenkinsBuildWithDetails;
        while ((jenkinsBuildWithDetails = jenkinsBuild.details()).isBuilding()) {
          Misc.quietSleep((int) (Math.max(
              5000, jenkinsBuildWithDetails.getDuration() - jenkinsBuildWithDetails.getEstimatedDuration())));
        }
        jenkinsExecutionResponse.setJobUrl(jenkinsBuild.getUrl());

        BuildResult buildResult = jenkinsBuildWithDetails.getResult();
        jenkinsExecutionResponse.setJenkinsResult(buildResult.toString());

        if (buildResult == BuildResult.SUCCESS || buildResult == BuildResult.UNSTABLE) {
          if (!isEmpty(evaluatedFilePathsForAssertion)) {
            for (Entry<String, String> entry : evaluatedFilePathsForAssertion.entrySet()) {
              String filePathForAssertion = entry.getKey();
              String assertion = entry.getValue();

              Pattern pattern =
                  Pattern.compile(filePathForAssertion.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
              Optional<Artifact> artifactOptional =
                  jenkinsBuildWithDetails.getArtifacts()
                      .stream()
                      .filter(artifact -> pattern.matcher(artifact.getRelativePath()).matches())
                      .findFirst();
              if (artifactOptional.isPresent()) {
                String data = CharStreams.toString(
                    new InputStreamReader(jenkinsBuildWithDetails.downloadArtifact(artifactOptional.get())));
                FilePathAssertionEntry filePathAssertionEntry =
                    new FilePathAssertionEntry(artifactOptional.get().getRelativePath(), assertion, data);
                filePathAssertionEntry.setStatus(
                    Boolean.TRUE.equals(context.evaluateExpression(assertion, filePathAssertionEntry)) ? Status.SUCCESS
                                                                                                       : Status.FAILED);
                jenkinsExecutionResponse.getFilePathAssertionMap().add(filePathAssertionEntry);
              } else {
                executionStatus = ExecutionStatus.FAILED;
                jenkinsExecutionResponse.getFilePathAssertionMap().add(
                    new FilePathAssertionEntry(filePathForAssertion, assertion, Status.NOT_FOUND));
              }
            }
          }
        } else {
          executionStatus = ExecutionStatus.FAILED;
        }
      } catch (Exception e) {
        logger.warn("Exception: ", e);
        if (e instanceof WingsException) {
          WingsException ex = (WingsException) e;
          errorMessage =
              Joiner.on(",").join(ex.getResponseMessageList()
                                      .stream()
                                      .map(responseMessage
                                          -> ResponseCodeCache.getInstance()
                                                 .getResponseMessage(responseMessage.getCode(), ex.getParams())
                                                 .getMessage())
                                      .collect(toList()));
        } else {
          errorMessage = e.getMessage();
        }
        executionStatus = executionStatus.FAILED;
        jenkinsExecutionResponse.setErrorMessage(errorMessage);
      }
      jenkinsExecutionResponse.setExecutionStatus(executionStatus);
      waitNotifyEngine.notify(finalActivityId, jenkinsExecutionResponse);
    });

    return anExecutionResponse()
        .withAsync(true)
        .withStateExecutionData(jenkinsExecutionData)
        .withCorrelationIds(Collections.singletonList(activityId))
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    JenkinsExecutionResponse jenkinsExecutionResponse = (JenkinsExecutionResponse) response.values().iterator().next();
    updateActivityStatus(jenkinsExecutionResponse.getActivityId(), context.getApp().getUuid(),
        jenkinsExecutionResponse.getExecutionStatus());
    JenkinsExecutionData jenkinsExecutionData = (JenkinsExecutionData) context.getStateExecutionData();
    jenkinsExecutionData.setFilePathAssertionMap(jenkinsExecutionResponse.getFilePathAssertionMap());
    jenkinsExecutionData.setJobStatus(jenkinsExecutionResponse.getJenkinsResult());
    jenkinsExecutionData.setBuildUrl(jenkinsExecutionResponse.getJobUrl());
    return anExecutionResponse()
        .withExecutionStatus(jenkinsExecutionResponse.getExecutionStatus())
        .withStateExecutionData(jenkinsExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  protected String createActivity(ExecutionContext executionContext) {
    Application app = executionContext.getApp();
    Environment env = executionContext.getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);

    Activity.Builder activityBuilder =
        anActivity()
            .withAppId(app.getUuid())
            .withApplicationName(app.getName())
            .withEnvironmentId(env.getUuid())
            .withEnvironmentName(env.getName())
            .withEnvironmentType(env.getEnvironmentType())
            .withCommandName(getName())
            .withType(Type.Verification)
            .withWorkflowType(executionContext.getWorkflowType())
            .withWorkflowExecutionName(executionContext.getWorkflowExecutionName())
            .withStateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .withStateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .withCommandType(getStateType())
            .withWorkflowExecutionId(executionContext.getWorkflowExecutionId());

    if (instanceElement != null) {
      activityBuilder.withServiceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .withServiceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .withServiceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .withServiceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .withServiceInstanceId(instanceElement.getUuid())
          .withHostName(instanceElement.getHostElement().getHostName());
    }

    return activityService.save(activityBuilder.build()).getUuid();
  }

  protected void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  public static final class JenkinsExecutionResponse implements NotifyResponseData {
    private ExecutionStatus executionStatus;
    private String jenkinsResult;
    private String errorMessage;
    private String activityId;
    private String jobUrl;
    private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();

    /**
     * Getter for property 'jobUrl'.
     *
     * @return Value for property 'jobUrl'.
     */
    public String getJobUrl() {
      return jobUrl;
    }

    /**
     * Setter for property 'jobUrl'.
     *
     * @param jobUrl Value to set for property 'jobUrl'.
     */
    public void setJobUrl(String jobUrl) {
      this.jobUrl = jobUrl;
    }

    /**
     * Getter for property 'executionStatus'.
     *
     * @return Value for property 'executionStatus'.
     */
    public ExecutionStatus getExecutionStatus() {
      return executionStatus;
    }

    /**
     * Setter for property 'executionStatus'.
     *
     * @param executionStatus Value to set for property 'executionStatus'.
     */
    public void setExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
    }

    /**
     * Getter for property 'jenkinsResult'.
     *
     * @return Value for property 'jenkinsResult'.
     */
    public String getJenkinsResult() {
      return jenkinsResult;
    }

    /**
     * Setter for property 'jenkinsResult'.
     *
     * @param jenkinsResult Value to set for property 'jenkinsResult'.
     */
    public void setJenkinsResult(String jenkinsResult) {
      this.jenkinsResult = jenkinsResult;
    }

    /**
     * Getter for property 'errorMessage'.
     *
     * @return Value for property 'errorMessage'.
     */
    public String getErrorMessage() {
      return errorMessage;
    }

    /**
     * Setter for property 'errorMessage'.
     *
     * @param errorMessage Value to set for property 'errorMessage'.
     */
    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    /**
     * Getter for property 'activityId'.
     *
     * @return Value for property 'activityId'.
     */
    public String getActivityId() {
      return activityId;
    }

    /**
     * Setter for property 'activityId'.
     *
     * @param activityId Value to set for property 'activityId'.
     */
    public void setActivityId(String activityId) {
      this.activityId = activityId;
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

    public static final class Builder {
      private ExecutionStatus executionStatus;
      private String jenkinsResult;
      private String errorMessage;
      private String activityId;
      private String jobUrl;
      private List<FilePathAssertionEntry> filePathAssertionMap = Lists.newArrayList();

      private Builder() {}

      public static Builder aJenkinsExecutionResponse() {
        return new Builder();
      }

      public Builder withExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
        return this;
      }

      public Builder withJenkinsResult(String jenkinsResult) {
        this.jenkinsResult = jenkinsResult;
        return this;
      }

      public Builder withErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
      }

      public Builder withActivityId(String activityId) {
        this.activityId = activityId;
        return this;
      }

      public Builder withJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
        return this;
      }

      public Builder withFilePathAssertionMap(List<FilePathAssertionEntry> filePathAssertionMap) {
        this.filePathAssertionMap = filePathAssertionMap;
        return this;
      }

      public Builder but() {
        return aJenkinsExecutionResponse()
            .withExecutionStatus(executionStatus)
            .withJenkinsResult(jenkinsResult)
            .withErrorMessage(errorMessage)
            .withActivityId(activityId)
            .withJobUrl(jobUrl)
            .withFilePathAssertionMap(filePathAssertionMap);
      }

      public JenkinsExecutionResponse build() {
        JenkinsExecutionResponse jenkinsExecutionResponse = new JenkinsExecutionResponse();
        jenkinsExecutionResponse.setExecutionStatus(executionStatus);
        jenkinsExecutionResponse.setJenkinsResult(jenkinsResult);
        jenkinsExecutionResponse.setErrorMessage(errorMessage);
        jenkinsExecutionResponse.setActivityId(activityId);
        jenkinsExecutionResponse.setJobUrl(jobUrl);
        jenkinsExecutionResponse.setFilePathAssertionMap(filePathAssertionMap);
        return jenkinsExecutionResponse;
      }
    }
  }

  public static class ParameterEntry {
    @Attributes(title = "Parameter Name") String key;
    @Attributes(title = "Parameter Value") String value;

    /**
     * Getter for property 'filePath'.
     *
     * @return Value for property 'filePath'.
     */
    public String getKey() {
      return key;
    }

    /**
     * Setter for property 'filePath'.
     *
     * @param key Value to set for property 'filePath'.
     */
    public void setKey(String key) {
      this.key = key;
    }

    /**
     * Getter for property 'assertion'.
     *
     * @return Value for property 'assertion'.
     */
    public String getValue() {
      return value;
    }

    /**
     * Setter for property 'assertion'.
     *
     * @param value Value to set for property 'assertion'.
     */
    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class FilePathAssertionEntry {
    @Attributes(title = "File/Artifact Path") String filePath;
    @Attributes(title = "Assertion") String assertion;
    @SchemaIgnore Status status;
    @SchemaIgnore String fileData;

    public FilePathAssertionEntry() {}

    public FilePathAssertionEntry(String filePath, String assertion, String fileData) {
      this.filePath = filePath;
      this.assertion = assertion;
      this.fileData = fileData;
    }

    public FilePathAssertionEntry(String filePath, String assertion, Status status) {
      this.filePath = filePath;
      this.assertion = assertion;
      this.status = status;
    }

    /**
     * Getter for property 'fileData'.
     *
     * @return Value for property 'fileData'.
     */
    public String getFileData() {
      return fileData;
    }

    /**
     * Setter for property 'fileData'.
     *
     * @param fileData Value to set for property 'fileData'.
     */
    public void setFileData(String fileData) {
      this.fileData = fileData;
    }

    /**
     * Getter for property 'filePath'.
     *
     * @return Value for property 'filePath'.
     */
    public String getFilePath() {
      return filePath;
    }

    /**
     * Setter for property 'filePath'.
     *
     * @param filePath Value to set for property 'filePath'.
     */
    public void setFilePath(String filePath) {
      this.filePath = filePath;
    }

    /**
     * Getter for property 'assertion'.
     *
     * @return Value for property 'assertion'.
     */
    public String getAssertion() {
      return assertion;
    }

    /**
     * Setter for property 'assertion'.
     *
     * @param assertion Value to set for property 'assertion'.
     */
    public void setAssertion(String assertion) {
      this.assertion = assertion;
    }

    /**
     * Getter for property 'status'.
     *
     * @return Value for property 'status'.
     */
    public Status getStatus() {
      return status;
    }

    /**
     * Setter for property 'status'.
     *
     * @param status Value to set for property 'status'.
     */
    public void setStatus(Status status) {
      this.status = status;
    }

    /**
     * Xml format.
     *
     * @return true, if successful
     */
    public boolean xmlFormat() {
      try {
        document();
        return true;
      } catch (Exception e) {
        return false;
      }
    }

    /**
     * Xpath.
     *
     * @param path the path
     * @return the string
     */
    public String xpath(String path) {
      try {
        return XmlUtils.xpath(document(), path);
      } catch (Exception e) {
        return null;
      }
    }

    private Document document() throws ParserConfigurationException, SAXException, IOException {
      return XmlUtils.parse(fileData);
    }

    public enum Status { NOT_FOUND, SUCCESS, FAILED }
  }
}
