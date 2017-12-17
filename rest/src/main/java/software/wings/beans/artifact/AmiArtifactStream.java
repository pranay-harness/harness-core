package software.wings.beans.artifact;

import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.EmbeddedUser;
import software.wings.utils.Misc;
import software.wings.utils.Util;

import java.util.Date;
import java.util.List;
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
      return String.format("%s_%s_%s", getRegion(), amiName, getDateFormat().format(new Date()));
    }
    return String.format("%s_%s_%s", getSourceName(), amiName);
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
    Multimap<String, String> multiTags = ArrayListMultimap.create();
    if (tags != null) {
      tags.forEach(tag -> multiTags.put(tag.getKey(), tag.getValue()));
    }
    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withRegion(region)
        .withTags(multiTags)
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
  }
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String awsCloudProviderName;
    private String region;
    private List<String> tags;

    public static final class Builder {
      private String awsCloudProviderName;
      private List<String> tags;
      private String sourceName;
      private String region;
      private String settingName;
      private boolean autoApproveForProduction = false;
      private String type;
      private boolean metadataOnly = false;

      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      public Builder withAwsCloudProviderName(String awsCloudProviderName) {
        this.awsCloudProviderName = awsCloudProviderName;
        return this;
      }

      public Builder withTags(List<String> tags) {
        this.tags = tags;
        return this;
      }

      public Builder withSourceName(String sourceName) {
        this.sourceName = sourceName;
        return this;
      }

      public Builder withRegion(String region) {
        this.region = region;
        return this;
      }

      public Builder withSettingName(String settingName) {
        this.settingName = settingName;
        return this;
      }

      public Builder withAutoApproveForProduction(boolean autoApproveForProduction) {
        this.autoApproveForProduction = autoApproveForProduction;
        return this;
      }

      public Builder withType(String type) {
        this.type = type;
        return this;
      }

      public Builder withMetadataOnly(boolean metadataOnly) {
        this.metadataOnly = metadataOnly;
        return this;
      }

      public Builder but() {
        return aYaml()
            .withAwsCloudProviderName(awsCloudProviderName)
            .withTags(tags)
            .withSourceName(sourceName)
            .withRegion(region)
            .withSettingName(settingName)
            .withAutoApproveForProduction(autoApproveForProduction)
            .withType(type)
            .withMetadataOnly(metadataOnly);
      }

      public Yaml build() {
        Yaml yaml = new Yaml();
        yaml.setAwsCloudProviderName(awsCloudProviderName);
        yaml.setSourceName(sourceName);
        yaml.setRegion(region);
        yaml.setTags(tags);
        yaml.setSettingName(settingName);
        yaml.setAutoApproveForProduction(autoApproveForProduction);
        yaml.setType(type);
        yaml.setMetadataOnly(metadataOnly);
        return yaml;
      }
    }
  }
}
