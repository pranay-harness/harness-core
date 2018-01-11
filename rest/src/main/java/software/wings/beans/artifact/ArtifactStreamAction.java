package software.wings.beans.artifact;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Embedded;
import software.wings.beans.WorkflowType;

import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 10/17/16.
 */
@Embedded
public class ArtifactStreamAction {
  @NotNull private String uuid;
  @NotNull private WorkflowType workflowType;
  @NotEmpty private String workflowId;
  private String workflowName;
  private String envId;
  private String envName;
  private boolean customAction;

  private String cronExpression;
  private String cronDescription;
  private String actionSummary; // TODO:: remove once UI stops using it.

  private String artifactFilter;
  private boolean webHook;
  private String webHookToken;
  private String requestBody;

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /**
   * Gets workflow id.
   *
   * @return the workflow id
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets workflow id.
   *
   * @param workflowId the workflow id
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Is custom action boolean.
   *
   * @return the boolean
   */
  public boolean isCustomAction() {
    return customAction;
  }

  /**
   * Sets custom action.
   *
   * @param customAction the custom action
   */
  public void setCustomAction(boolean customAction) {
    this.customAction = customAction;
  }

  /**
   * Gets cron expression.
   *
   * @return the cron expression
   */
  public String getCronExpression() {
    return cronExpression;
  }

  /**
   * Sets cron expression.
   *
   * @param cronExpression the cron expression
   */
  public void setCronExpression(String cronExpression) {
    this.cronExpression = cronExpression;
  }

  /**
   * Gets action summary.
   *
   * @return the action summary
   */
  public String getActionSummary() {
    return actionSummary;
  }

  /**
   * Sets action summary.
   *
   * @param actionSummary the action summary
   */
  public void setActionSummary(String actionSummary) {
    this.actionSummary = actionSummary;
  }

  /**
   * Gets translated cron.
   *
   * @return the translated cron
   */
  public String getCronDescription() {
    return cronDescription;
  }

  /**
   * Sets translated cron.
   *
   * @param cronDescription the translated cron
   */
  public void setCronDescription(String cronDescription) {
    this.cronDescription = cronDescription;
  }

  /**
   * Gets workflow name.
   *
   * @return the workflow name
   */
  public String getWorkflowName() {
    return workflowName;
  }

  /**
   * Sets workflow name.
   *
   * @param workflowName the workflow name
   */
  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  /**
   * Gets env name.
   *
   * @return the env name
   */
  public String getEnvName() {
    return envName;
  }

  /**
   * Sets env name.
   *
   * @param envName the env name
   */
  public void setEnvName(String envName) {
    this.envName = envName;
  }

  /**
   * Get artifact Path Regex
   *
   * @return artifact filter
   */
  public String getArtifactFilter() {
    return artifactFilter;
  }

  /**
   * Set artifact filter
   *
   * @param artifactFilter the artifact filter
   */
  public void setArtifactFilter(String artifactFilter) {
    this.artifactFilter = artifactFilter;
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowType, workflowId, workflowName, envId, envName, customAction, cronExpression,
        cronDescription, actionSummary, artifactFilter);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ArtifactStreamAction other = (ArtifactStreamAction) obj;
    return Objects.equals(this.workflowType, other.workflowType) && Objects.equals(this.workflowId, other.workflowId)
        && Objects.equals(this.workflowName, other.workflowName) && Objects.equals(this.envId, other.envId)
        && Objects.equals(this.envName, other.envName) && Objects.equals(this.customAction, other.customAction)
        && Objects.equals(this.cronExpression, other.cronExpression)
        && Objects.equals(this.cronDescription, other.cronDescription)
        && Objects.equals(this.actionSummary, other.actionSummary)
        && Objects.equals(this.artifactFilter, other.artifactFilter);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("workflowType", workflowType)
        .add("workflowId", workflowId)
        .add("workflowName", workflowName)
        .add("envId", envId)
        .add("envName", envName)
        .add("customAction", customAction)
        .add("cronExpression", cronExpression)
        .add("cronDescription", cronDescription)
        .add("actionSummary", actionSummary)
        .add("artifactFilter", artifactFilter)
        .toString();
  }

  /**
   * Gets uuid.
   *
   * @return the uuid
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Sets uuid.
   *
   * @param uuid the uuid
   */
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   * Gets webHook token.
   *
   * @return the webHook token
   */
  public String getWebHookToken() {
    return webHookToken;
  }

  /**
   * Sets webHook token.
   *
   * @param webHookToken the webHook token
   */
  public void setWebHookToken(String webHookToken) {
    this.webHookToken = webHookToken;
  }

  /**
   * Gets request body template.
   *
   * @return the request body template
   */
  public String getRequestBody() {
    return requestBody;
  }

  /**
   * Sets request body template.
   *
   * @param requestBody the request body template
   */
  public void setRequestBody(String requestBody) {
    this.requestBody = requestBody;
  }

  public boolean isWebHook() {
    return webHook;
  }

  public void setWebHook(boolean webHook) {
    this.webHook = webHook;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String uuid;
    private WorkflowType workflowType;
    private String workflowId;
    private String workflowName;
    private String envId;
    private String envName;
    private boolean customAction;
    private String cronExpression;
    private String cronDescription;
    private String actionSummary; // TODO:: remove once UI stops using it.
    private String artifactFilter;

    private Builder() {}

    /**
     * An artifact stream action builder.
     *
     * @return the builder
     */
    public static Builder anArtifactStreamAction() {
      return new Builder();
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With workflow type builder.
     *
     * @param workflowType the workflow type
     * @return the builder
     */
    public Builder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    /**
     * With workflow id builder.
     *
     * @param workflowId the workflow id
     * @return the builder
     */
    public Builder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    /**
     * With workflow name builder.
     *
     * @param workflowName the workflow name
     * @return the builder
     */
    public Builder withWorkflowName(String workflowName) {
      this.workflowName = workflowName;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With env name builder.
     *
     * @param envName the env name
     * @return the builder
     */
    public Builder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    /**
     * With custom action builder.
     *
     * @param customAction the custom action
     * @return the builder
     */
    public Builder withCustomAction(boolean customAction) {
      this.customAction = customAction;
      return this;
    }

    /**
     * With cron expression builder.
     *
     * @param cronExpression the cron expression
     * @return the builder
     */
    public Builder withCronExpression(String cronExpression) {
      this.cronExpression = cronExpression;
      return this;
    }

    /**
     * With cron description builder.
     *
     * @param cronDescription the cron description
     * @return the builder
     */
    public Builder withCronDescription(String cronDescription) {
      this.cronDescription = cronDescription;
      return this;
    }

    /**
     * With action summary builder.
     *
     * @param actionSummary the action summary
     * @return the builder
     */
    public Builder withActionSummary(String actionSummary) {
      this.actionSummary = actionSummary;
      return this;
    }

    /**
     * With artifact filter builder.
     *
     * @param artifactFilter the artifact filter
     * @return the builder
     */
    public Builder withArtifactFilter(String artifactFilter) {
      this.artifactFilter = artifactFilter;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactStreamAction()
          .withUuid(uuid)
          .withWorkflowType(workflowType)
          .withWorkflowId(workflowId)
          .withWorkflowName(workflowName)
          .withEnvId(envId)
          .withEnvName(envName)
          .withCustomAction(customAction)
          .withCronExpression(cronExpression)
          .withCronDescription(cronDescription)
          .withActionSummary(actionSummary)
          .withArtifactFilter(artifactFilter);
    }

    /**
     * Build artifact stream action.
     *
     * @return the artifact stream action
     */
    public ArtifactStreamAction build() {
      ArtifactStreamAction artifactStreamAction = new ArtifactStreamAction();
      artifactStreamAction.setUuid(uuid);
      artifactStreamAction.setWorkflowType(workflowType);
      artifactStreamAction.setWorkflowId(workflowId);
      artifactStreamAction.setWorkflowName(workflowName);
      artifactStreamAction.setEnvId(envId);
      artifactStreamAction.setEnvName(envName);
      artifactStreamAction.setCustomAction(customAction);
      artifactStreamAction.setCronExpression(cronExpression);
      artifactStreamAction.setCronDescription(cronDescription);
      artifactStreamAction.setActionSummary(actionSummary);
      artifactStreamAction.setArtifactFilter(artifactFilter);
      return artifactStreamAction;
    }
  }
}
