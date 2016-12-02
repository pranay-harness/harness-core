package software.wings.beans;

import com.google.common.collect.Sets;

import com.fasterxml.jackson.annotation.JsonTypeName;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;

import java.util.List;
import java.util.Set;

/**
 * Created by anubhaw on 4/13/16.
 */
@JsonTypeName("HTTP")
public class FileUrlSource extends ArtifactStream {
  private String url;

  /**
   * Instantiates a new file url source.
   */
  public FileUrlSource() {
    super(SourceType.HTTP);
  }

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String url;
    private String sourceName;
    private boolean autoDownload = false;
    private boolean autoApproveForProduction = false;
    private List<ArtifactStreamAction> postDownloadActions;
    private Artifact lastArtifact;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A file url source builder.
     *
     * @return the builder
     */
    public static Builder aFileUrlSource() {
      return new Builder();
    }

    /**
     * With url builder.
     *
     * @param url the url
     * @return the builder
     */
    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * With source name builder.
     *
     * @param sourceName the source name
     * @return the builder
     */
    public Builder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    /**
     * With auto download builder.
     *
     * @param autoDownload the auto download
     * @return the builder
     */
    public Builder withAutoDownload(boolean autoDownload) {
      this.autoDownload = autoDownload;
      return this;
    }

    /**
     * With auto approve for production builder.
     *
     * @param autoApproveForProduction the auto approve for production
     * @return the builder
     */
    public Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    /**
     * With post download actions builder.
     *
     * @param postDownloadActions the post download actions
     * @return the builder
     */
    public Builder withPostDownloadActions(List<ArtifactStreamAction> postDownloadActions) {
      this.postDownloadActions = postDownloadActions;
      return this;
    }

    /**
     * With last artifact builder.
     *
     * @param lastArtifact the last artifact
     * @return the builder
     */
    public Builder withLastArtifact(Artifact lastArtifact) {
      this.lastArtifact = lastArtifact;
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
      return aFileUrlSource()
          .withUrl(url)
          .withSourceName(sourceName)
          .withAutoDownload(autoDownload)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withPostDownloadActions(postDownloadActions)
          .withLastArtifact(lastArtifact)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build file url source.
     *
     * @return the file url source
     */
    public FileUrlSource build() {
      FileUrlSource fileUrlSource = new FileUrlSource();
      fileUrlSource.setUrl(url);
      fileUrlSource.setSourceName(sourceName);
      fileUrlSource.setAutoDownload(autoDownload);
      fileUrlSource.setAutoApproveForProduction(autoApproveForProduction);
      fileUrlSource.setStreamActions(postDownloadActions);
      fileUrlSource.setLastArtifact(lastArtifact);
      fileUrlSource.setUuid(uuid);
      fileUrlSource.setAppId(appId);
      fileUrlSource.setCreatedBy(createdBy);
      fileUrlSource.setCreatedAt(createdAt);
      fileUrlSource.setLastUpdatedBy(lastUpdatedBy);
      fileUrlSource.setLastUpdatedAt(lastUpdatedAt);
      return fileUrlSource;
    }
  }
}
