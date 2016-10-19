package software.wings.beans;

import static java.util.Arrays.asList;
import static software.wings.beans.ApprovalNotification.ApprovalStage.APPROVED;
import static software.wings.beans.ApprovalNotification.ApprovalStage.PENDING;
import static software.wings.beans.ApprovalNotification.ApprovalStage.REJECTED;
import static software.wings.beans.Notification.NotificationType.APPROVAL;
import static software.wings.beans.NotificationAction.Builder.aNotificationAction;
import static software.wings.beans.NotificationAction.NotificationActionType.APPROVE;
import static software.wings.beans.NotificationAction.NotificationActionType.REJECT;

import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.artifact.Artifact.Status;
import software.wings.beans.NotificationAction.NotificationActionType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ArtifactService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 7/25/16.
 */
@JsonTypeName("APPROVAL")
public class ApprovalNotification extends ActionableNotification {
  @NotEmpty private String entityName;
  @NotNull private ApprovalStage stage = PENDING;
  private String artifactStreamId;
  @Inject @Transient private transient WingsPersistence wingsPersistence;
  @Inject @Transient private transient ArtifactService artifactService;

  /**
   * Instantiates a new Approval notification.
   */
  public ApprovalNotification() {
    super(APPROVAL,
        asList(aNotificationAction().withName("Approve").withType(APPROVE).withPrimary(true).build(),
            aNotificationAction().withName("Reject").withType(REJECT).withPrimary(false).build()));
  }

  @Override
  public boolean performAction(NotificationActionType actionType) {
    if (EntityType.ARTIFACT.equals(getEntityType())) {
      artifactService.updateStatus(
          getEntityId(), getAppId(), actionType.equals(APPROVE) ? Status.APPROVED : Status.REJECTED);
    }
    wingsPersistence.updateFields(ApprovalNotification.class, getUuid(),
        ImmutableMap.of("stage", actionType.equals(APPROVE) ? APPROVED : REJECTED));
    return true;
  }

  /**
   * Gets entity name.
   *
   * @return the entity name
   */
  public String getEntityName() {
    return entityName;
  }

  /**
   * Sets entity name.
   *
   * @param entityName the entity name
   */
  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  /**
   * Gets stage.
   *
   * @return the stage
   */
  public ApprovalStage getStage() {
    return stage;
  }

  /**
   * Sets stage.
   *
   * @param stage the stage
   */
  public void setStage(ApprovalStage stage) {
    this.stage = stage;
  }

  /**
   * Gets artifact stream id.
   *
   * @return the artifact stream id
   */
  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  /**
   * Sets artifact stream id.
   *
   * @param artifactStreamId the artifact stream id
   */
  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  /**
   * The enum Approval stage.
   */
  public enum ApprovalStage {
    /**
     * Pending approval stage.
     */
    PENDING, /**
              * Accepted approval stage.
              */
    APPROVED, /**
               * Rejected approval stage.
               */
    REJECTED
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String entityName;
    private ApprovalStage stage = PENDING;
    private String artifactStreamId;
    private String environmentId;
    private String entityId;
    private EntityType entityType;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An approval notification builder.
     *
     * @return the builder
     */
    public static Builder anApprovalNotification() {
      return new Builder();
    }

    /**
     * With entity name builder.
     *
     * @param entityName the entity name
     * @return the builder
     */
    public Builder withEntityName(String entityName) {
      this.entityName = entityName;
      return this;
    }

    /**
     * With stage builder.
     *
     * @param stage the stage
     * @return the builder
     */
    public Builder withStage(ApprovalStage stage) {
      this.stage = stage;
      return this;
    }

    /**
     * With artifact stream id builder.
     *
     * @param artifactStreamId the artifact stream id
     * @return the builder
     */
    public Builder withArtifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    /**
     * With environment id builder.
     *
     * @param environmentId the environment id
     * @return the builder
     */
    public Builder withEnvironmentId(String environmentId) {
      this.environmentId = environmentId;
      return this;
    }

    /**
     * With entity id builder.
     *
     * @param entityId the entity id
     * @return the builder
     */
    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    /**
     * With entity type builder.
     *
     * @param entityType the entity type
     * @return the builder
     */
    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
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
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anApprovalNotification()
          .withEntityName(entityName)
          .withStage(stage)
          .withArtifactStreamId(artifactStreamId)
          .withEnvironmentId(environmentId)
          .withEntityId(entityId)
          .withEntityType(entityType)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build approval notification.
     *
     * @return the approval notification
     */
    public ApprovalNotification build() {
      ApprovalNotification approvalNotification = new ApprovalNotification();
      approvalNotification.setEntityName(entityName);
      approvalNotification.setStage(stage);
      approvalNotification.setArtifactStreamId(artifactStreamId);
      approvalNotification.setEnvironmentId(environmentId);
      approvalNotification.setEntityId(entityId);
      approvalNotification.setEntityType(entityType);
      approvalNotification.setUuid(uuid);
      approvalNotification.setAppId(appId);
      approvalNotification.setCreatedBy(createdBy);
      approvalNotification.setCreatedAt(createdAt);
      approvalNotification.setLastUpdatedBy(lastUpdatedBy);
      approvalNotification.setLastUpdatedAt(lastUpdatedAt);
      return approvalNotification;
    }
  }
}
