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
  @NotNull private WorkflowType workflowType;
  @NotEmpty private String workflowId;
  private String envId;
  @NotNull private boolean customAction = false;
  private String cronExpression;
  private String actionSummary;
  private String jobId;

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
   * Gets job id.
   *
   * @return the job id
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * Sets job id.
   *
   * @param jobId the job id
   */
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(workflowType, workflowId, envId, customAction, cronExpression, jobId);
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
        && Objects.equals(this.envId, other.envId) && Objects.equals(this.customAction, other.customAction)
        && Objects.equals(this.cronExpression, other.cronExpression) && Objects.equals(this.jobId, other.jobId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("workflowType", workflowType)
        .add("workflowId", workflowId)
        .add("envId", envId)
        .add("customAction", customAction)
        .add("cronExpression", cronExpression)
        .add("jobId", jobId)
        .toString();
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
   * The type Builder.
   */
  public static final class Builder {
    private WorkflowType workflowType;
    private String workflowId;
    private String envId;
    private boolean customAction = false;
    private String cronExpression;
    private String actionSummary;
    private String jobId;

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
     * With job id builder.
     *
     * @param jobId the job id
     * @return the builder
     */
    public Builder withJobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anArtifactStreamAction()
          .withWorkflowType(workflowType)
          .withWorkflowId(workflowId)
          .withEnvId(envId)
          .withCustomAction(customAction)
          .withCronExpression(cronExpression)
          .withActionSummary(actionSummary)
          .withJobId(jobId);
    }

    /**
     * Build artifact stream action.
     *
     * @return the artifact stream action
     */
    public ArtifactStreamAction build() {
      ArtifactStreamAction artifactStreamAction = new ArtifactStreamAction();
      artifactStreamAction.setWorkflowType(workflowType);
      artifactStreamAction.setWorkflowId(workflowId);
      artifactStreamAction.setEnvId(envId);
      artifactStreamAction.setCustomAction(customAction);
      artifactStreamAction.setCronExpression(cronExpression);
      artifactStreamAction.setActionSummary(actionSummary);
      artifactStreamAction.setJobId(jobId);
      return artifactStreamAction;
    }
  }
}
