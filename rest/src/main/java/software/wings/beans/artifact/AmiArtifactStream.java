package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;

import com.google.common.base.Joiner;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.EmbeddedUser;
import software.wings.utils.Misc;
import software.wings.utils.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 12/14/17.
 */
@JsonTypeName("AMI")
public class AmiArtifactStream extends ArtifactStream {
  private String region;

  private List<Tag> tags;

  /**
   * AmiArtifactStream
   */
  public AmiArtifactStream() {
    super(ArtifactStreamType.AMI.name());
    super.setAutoApproveForProduction(true);
    super.setAutoDownload(true);
    super.setMetadataOnly(true);
  }

  @Override
  public String getArtifactDisplayName(String amiName) {
    if (Misc.isNullOrEmpty(tags)) {
      return String.format("%s_%s", getRegion(), amiName);
    }
    return String.format("%s_%s", getSourceName(), amiName);
  }

  @Override
  public String generateName() {
    return Util.normalize(generateSourceName());
  }

  @Override
  public String generateSourceName() {
    if (tags == null || tags.size() == 0) {
      return region;
    }
    Set<String> tagNames = tags.stream().map(Tag::getKey).collect(Collectors.toSet());
    return region + ":" + Joiner.on("_").join(tagNames);
  }

  @Override
  @SchemaIgnore
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    Map<String, List<String>> tagMap = new HashMap<>();
    if (tags != null) {
      Map<String, List<Tag>> collect = tags.stream().collect(Collectors.groupingBy(Tag::getKey));
      tags.stream()
          .collect(Collectors.groupingBy(Tag::getKey))
          .keySet()
          .forEach(s -> tagMap.put(s, collect.get(s).stream().map(tag -> tag.value).collect(Collectors.toList())));
    }
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withRegion(region)
        .withTags(tagMap)
        .build();
  }

  @Override
  public ArtifactStream clone() {
    return null;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public static class Tag {
    private String key;
    private String value;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public static final class Builder {
    private AmiArtifactStream amiArtifactStream;

    private Builder() {
      amiArtifactStream = new AmiArtifactStream();
    }

    public static Builder anAmiArtifactStream() {
      return new Builder();
    }

    public Builder withRegion(String region) {
      amiArtifactStream.setRegion(region);
      return this;
    }

    public Builder withTags(List<Tag> tags) {
      amiArtifactStream.setTags(tags);
      return this;
    }

    public Builder withUuid(String uuid) {
      amiArtifactStream.setUuid(uuid);
      return this;
    }

    public Builder withAppId(String appId) {
      amiArtifactStream.setAppId(appId);
      return this;
    }

    public Builder withSourceName(String sourceName) {
      amiArtifactStream.setSourceName(sourceName);
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      amiArtifactStream.setCreatedBy(createdBy);
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      amiArtifactStream.setCreatedAt(createdAt);
      return this;
    }

    public Builder withSettingId(String settingId) {
      amiArtifactStream.setSettingId(settingId);
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      amiArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      amiArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      return this;
    }

    public Builder withName(String name) {
      amiArtifactStream.setName(name);
      return this;
    }

    public Builder withServiceId(String serviceId) {
      amiArtifactStream.setServiceId(serviceId);
      return this;
    }

    public Builder withStreamActions(List<ArtifactStreamAction> streamActions) {
      amiArtifactStream.setStreamActions(streamActions);
      return this;
    }

    public Builder but() {
      return anAmiArtifactStream()
          .withRegion(amiArtifactStream.getRegion())
          .withTags(amiArtifactStream.getTags())
          .withUuid(amiArtifactStream.getUuid())
          .withAppId(amiArtifactStream.getAppId())
          .withSourceName(amiArtifactStream.getSourceName())
          .withCreatedBy(amiArtifactStream.getCreatedBy())
          .withCreatedAt(amiArtifactStream.getCreatedAt())
          .withSettingId(amiArtifactStream.getSettingId())
          .withLastUpdatedBy(amiArtifactStream.getLastUpdatedBy())
          .withLastUpdatedAt(amiArtifactStream.getLastUpdatedAt())
          .withName(amiArtifactStream.getName())
          .withServiceId(amiArtifactStream.getServiceId())
          .withStreamActions(amiArtifactStream.getStreamActions());
    }

    public AmiArtifactStream build() {
      return amiArtifactStream;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class Yaml extends ArtifactStream.Yaml {
      private String awsCloudProviderName;
      private List<Tag> tags;
      private String region;

      @lombok.Builder
      public Yaml(String harnessApiVersion, String artifactServerName, boolean metadataOnly,
          String awsCloudProviderName, List<Tag> tags, String region) {
        super(AMI.name(), harnessApiVersion, artifactServerName, metadataOnly);
        this.awsCloudProviderName = awsCloudProviderName;
        this.tags = tags;
        this.region = region;
      }
    }
  }
}
