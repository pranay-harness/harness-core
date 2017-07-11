package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EmbeddedUser;
import software.wings.stencils.UIOrder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by srinivas on 3/31/17.
 */
@JsonTypeName("NEXUS")
public class NexusArtifactStream extends ArtifactStream {
  public String getJobname() {
    return jobname;
  }

  public void setJobname(String jobname) {
    this.jobname = jobname;
  }

  @UIOrder(4) @NotEmpty @Attributes(title = "Repository", required = true) private String jobname;

  @UIOrder(5) @NotEmpty @Attributes(title = "Group", required = true) private String groupId;

  @UIOrder(6) @NotEmpty @Attributes(title = "Artifact", required = true) private List<String> artifactPaths;

  /**
   * Instantiates a new Nexus artifact stream.
   */
  public NexusArtifactStream() {
    super(ArtifactStreamType.NEXUS.name());
  }

  @Override
  @SchemaIgnore
  public String getArtifactDisplayName(String buildNo) {
    return String.format("%s_%s_%s", getSourceName(), buildNo, getDateFormat().format(new Date()));
  }

  @Attributes(title = "Source Type")
  @Override
  public String getArtifactStreamType() {
    return super.getArtifactStreamType();
  }

  @Attributes(title = "Source Server")
  @Override
  public String getSettingId() {
    return super.getSettingId();
  }

  @UIOrder(7)
  @Attributes(title = "Auto-approved for Production")
  public boolean getAutoApproveForProduction() {
    return super.isAutoApproveForProduction();
  }

  /**
   * Gets artifact paths.
   *
   * @return the artifact paths
   */
  public List<String> getArtifactPaths() {
    return artifactPaths;
  }

  /**
   * Sets artifact paths.
   *
   * @param artifactPaths the artifact paths
   */
  public void setArtifactPaths(List<String> artifactPaths) {
    this.artifactPaths = artifactPaths;
  }

  /**
   * @return groupId
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Set Group Id
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  @Override
  @SchemaIgnore
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withJobName(jobname)
        .withGroupId(groupId)
        .withArtifactName(artifactPaths == null ? "" : artifactPaths.get(0))
        .build();
  }

  @Override
  public ArtifactStream clone() {
    return NexusArtifactStream.Builder.aNexusArtifactStream()
        .withAppId(getAppId())
        .withSourceName(getSourceName())
        .withSettingId(getSettingId())
        .withServiceId(getServiceId())
        .withAutoApproveForProduction(getAutoApproveForProduction())
        .withStreamActions(getStreamActions())
        .withJobname(getJobname())
        .withGroupId(getGroupId())
        .withArtifactPaths(getArtifactPaths())
        .build();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String jobname;
    private String groupId;
    private List<String> artifactPaths;
    private String sourceName;
    private String settingId;
    private String serviceId;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean autoApproveForProduction = false;
    private List<ArtifactStreamAction> streamActions = new ArrayList<>();

    private Builder() {}

    /**
     * A bamboo artifact stream builder.
     *
     * @return the builder
     */
    public static NexusArtifactStream.Builder aNexusArtifactStream() {
      return new NexusArtifactStream.Builder();
    }

    /**
     * With jobname builder.
     *
     * @param jobname the jobname
     * @return the builder
     */
    public NexusArtifactStream.Builder withJobname(String jobname) {
      this.jobname = jobname;
      return this;
    }

    /**
     * With groupId builder.
     *
     * @param groupId the groupId
     * @return the builder
     */
    public NexusArtifactStream.Builder withGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    /**
     * With artifact paths builder.
     *
     * @param artifactPaths the artifact paths
     * @return the builder
     */
    public NexusArtifactStream.Builder withArtifactPaths(List<String> artifactPaths) {
      this.artifactPaths = artifactPaths;
      return this;
    }

    /**
     * With source name builder.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public NexusArtifactStream.Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With setting id builder.
     *
     * @param settingId the setting id
     * @return the builder
     */
    public NexusArtifactStream.Builder withSettingId(String settingId) {
      this.settingId = settingId;
      return this;
    }

    /**
     * With service id builder.
     *
     * @param serviceId the service id
     * @return the builder
     */
    public NexusArtifactStream.Builder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public NexusArtifactStream.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public NexusArtifactStream.Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public NexusArtifactStream.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public NexusArtifactStream.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public NexusArtifactStream.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public NexusArtifactStream.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With auto approve for production builder.
     *
     * @param autoApproveForProduction the auto approve for production
     * @return the builder
     */
    public NexusArtifactStream.Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    /**
     * With stream actions builder.
     *
     * @param streamActions the stream actions
     * @return the builder
     */
    public NexusArtifactStream.Builder withStreamActions(List<ArtifactStreamAction> streamActions) {
      this.streamActions = streamActions;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public NexusArtifactStream.Builder but() {
      return aNexusArtifactStream()
          .withJobname(jobname)
          .withGroupId(groupId)
          .withArtifactPaths(artifactPaths)
          .withSourceName(sourceName)
          .withSettingId(settingId)
          .withServiceId(serviceId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withStreamActions(streamActions);
    }

    /**
     * Build bamboo artifact stream.
     *
     * @return the bamboo artifact stream
     */
    public NexusArtifactStream build() {
      NexusArtifactStream nexusArtifactStream = new NexusArtifactStream();
      nexusArtifactStream.setJobname(jobname);
      nexusArtifactStream.setGroupId(groupId);
      nexusArtifactStream.setArtifactPaths(artifactPaths);
      nexusArtifactStream.setSourceName(sourceName);
      nexusArtifactStream.setSettingId(settingId);
      nexusArtifactStream.setServiceId(serviceId);
      nexusArtifactStream.setUuid(uuid);
      nexusArtifactStream.setAppId(appId);
      nexusArtifactStream.setCreatedBy(createdBy);
      nexusArtifactStream.setCreatedAt(createdAt);
      nexusArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      nexusArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      nexusArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      nexusArtifactStream.setStreamActions(streamActions);
      return nexusArtifactStream;
    }
  }
}
