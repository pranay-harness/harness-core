package software.wings.beans.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;

import com.google.common.base.Joiner;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.NameValuePair;
import software.wings.utils.Util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * Created by sgurubelli on 12/14/17.
 */
@JsonTypeName("AMI")
public class AmiArtifactStream extends ArtifactStream {
  private String region;
  private String platform;
  private List<Tag> tags;
  private List<FilterClass> filters;

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
    if (isEmpty(tags)) {
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
    if (isEmpty(tags) && isEmpty(filters)) {
      return region;
    }
    List<String> tagFields = new ArrayList<>();
    if (tags != null) {
      for (Tag tag : tags) {
        tagFields.add(tag.getKey() + ":" + tag.getValue());
      }
    }

    List<String> filterFields = new ArrayList<>();
    if (filters != null) {
      for (FilterClass filterClass : filters) {
        filterFields.add(filterClass.getKey() + ":" + filterClass.getValue());
      }
    }

    return region + (tagFields.size() > 0 ? ":" + Joiner.on("_").join(tagFields) : "")
        + (filterFields.size() > 0 ? ":" + Joiner.on("_").join(filterFields) : "");
  }

  @Override
  @SchemaIgnore
  public ArtifactStreamAttributes getArtifactStreamAttributes() {
    Map<String, List<String>> tagMap = new HashMap<>();
    Map<String, String> filterMap = new HashMap<>();
    if (tags != null) {
      Map<String, List<Tag>> collect = tags.stream().collect(Collectors.groupingBy(Tag::getKey));
      tags.stream()
          .collect(Collectors.groupingBy(Tag::getKey))
          .keySet()
          .forEach(s -> tagMap.put(s, collect.get(s).stream().map(tag -> tag.value).collect(Collectors.toList())));
    }

    if (filters != null) {
      filters.stream().forEach(filter -> filterMap.put(filter.getKey(), filter.getValue()));
    }

    return anArtifactStreamAttributes()
        .withArtifactStreamType(getArtifactStreamType())
        .withRegion(region)
        .withTags(tagMap)
        .withFilters(filterMap)
        .withPlatform(platform)
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

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public List<Tag> getTags() {
    return tags;
  }

  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public List<FilterClass> getFilters() {
    return filters;
  }

  public void setFilters(List<FilterClass> filters) {
    this.filters = filters;
  }

  public static class FilterClass {
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends ArtifactStream.Yaml {
    private String platform;
    private String region;
    private List<NameValuePair.Yaml> tags = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, boolean metadataOnly, List<NameValuePair.Yaml> tags,
        String region, String platform) {
      super(AMI.name(), harnessApiVersion, serverName, metadataOnly);
      this.tags = tags;
      this.region = region;
      this.platform = platform;
    }
  }

  public static final class AmiArtifactStreamBuilder {
    public static String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";
    public static String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";
    public static String GLOBAL_ENV_ID = "__GLOBAL_ENV_ID__";
    private static DateFormat dateFormat = new SimpleDateFormat("HHMMSS");
    public transient String entityYamlPath; // TODO:: remove it with changeSet batching
    protected String appId;
    private String region;
    private String platform;
    private List<Tag> tags;
    private List<FilterClass> filters;
    private String artifactStreamType;
    private String uuid;
    private String sourceName;
    private EmbeddedUser createdBy;
    private String settingId;
    private long createdAt;
    private String name;
    private EmbeddedUser lastUpdatedBy;
    // auto populate name
    private boolean autoPopulate = true;
    private long lastUpdatedAt;
    private String serviceId;
    private List<String> keywords;
    private boolean autoDownload = true;
    private boolean autoApproveForProduction;
    private boolean metadataOnly;

    private AmiArtifactStreamBuilder() {}

    public static AmiArtifactStreamBuilder anAmiArtifactStream() {
      return new AmiArtifactStreamBuilder();
    }

    public AmiArtifactStreamBuilder withRegion(String region) {
      this.region = region;
      return this;
    }

    public AmiArtifactStreamBuilder withPlatform(String platform) {
      this.platform = platform;
      return this;
    }

    public AmiArtifactStreamBuilder withTags(List<Tag> tags) {
      this.tags = tags;
      return this;
    }

    public AmiArtifactStreamBuilder withFilters(List<FilterClass> filters) {
      this.filters = filters;
      return this;
    }

    public AmiArtifactStreamBuilder withGLOBAL_APP_ID(String GLOBAL_APP_ID) {
      this.GLOBAL_APP_ID = GLOBAL_APP_ID;
      return this;
    }

    public AmiArtifactStreamBuilder withDateFormat(DateFormat dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    public AmiArtifactStreamBuilder withGLOBAL_ACCOUNT_ID(String GLOBAL_ACCOUNT_ID) {
      this.GLOBAL_ACCOUNT_ID = GLOBAL_ACCOUNT_ID;
      return this;
    }

    public AmiArtifactStreamBuilder withGLOBAL_ENV_ID(String GLOBAL_ENV_ID) {
      this.GLOBAL_ENV_ID = GLOBAL_ENV_ID;
      return this;
    }

    public AmiArtifactStreamBuilder withArtifactStreamType(String artifactStreamType) {
      this.artifactStreamType = artifactStreamType;
      return this;
    }

    public AmiArtifactStreamBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public AmiArtifactStreamBuilder withSourceName(String sourceName) {
      this.sourceName = sourceName;
      return this;
    }

    public AmiArtifactStreamBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public AmiArtifactStreamBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public AmiArtifactStreamBuilder withSettingId(String settingId) {
      this.settingId = settingId;
      return this;
    }

    public AmiArtifactStreamBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public AmiArtifactStreamBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public AmiArtifactStreamBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public AmiArtifactStreamBuilder withAutoPopulate(boolean autoPopulate) {
      this.autoPopulate = autoPopulate;
      return this;
    }

    public AmiArtifactStreamBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public AmiArtifactStreamBuilder withServiceId(String serviceId) {
      this.serviceId = serviceId;
      return this;
    }

    public AmiArtifactStreamBuilder withKeywords(List<String> keywords) {
      this.keywords = keywords;
      return this;
    }

    public AmiArtifactStreamBuilder withAutoDownload(boolean autoDownload) {
      this.autoDownload = autoDownload;
      return this;
    }

    public AmiArtifactStreamBuilder withAutoApproveForProduction(boolean autoApproveForProduction) {
      this.autoApproveForProduction = autoApproveForProduction;
      return this;
    }

    public AmiArtifactStreamBuilder withEntityYamlPath(String entityYamlPath) {
      this.entityYamlPath = entityYamlPath;
      return this;
    }

    public AmiArtifactStreamBuilder withMetadataOnly(boolean metadataOnly) {
      this.metadataOnly = metadataOnly;
      return this;
    }

    public AmiArtifactStreamBuilder but() {
      return anAmiArtifactStream()
          .withRegion(region)
          .withPlatform(platform)
          .withTags(tags)
          .withFilters(filters)
          .withGLOBAL_APP_ID(GLOBAL_APP_ID)
          .withDateFormat(dateFormat)
          .withGLOBAL_ACCOUNT_ID(GLOBAL_ACCOUNT_ID)
          .withGLOBAL_ENV_ID(GLOBAL_ENV_ID)
          .withArtifactStreamType(artifactStreamType)
          .withUuid(uuid)
          .withSourceName(sourceName)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withSettingId(settingId)
          .withCreatedAt(createdAt)
          .withName(name)
          .withLastUpdatedBy(lastUpdatedBy)
          .withAutoPopulate(autoPopulate)
          .withLastUpdatedAt(lastUpdatedAt)
          .withServiceId(serviceId)
          .withKeywords(keywords)
          .withAutoDownload(autoDownload)
          .withAutoApproveForProduction(autoApproveForProduction)
          .withEntityYamlPath(entityYamlPath)
          .withMetadataOnly(metadataOnly);
    }

    public AmiArtifactStream build() {
      AmiArtifactStream amiArtifactStream = new AmiArtifactStream();
      amiArtifactStream.setRegion(region);
      amiArtifactStream.setPlatform(platform);
      amiArtifactStream.setTags(tags);
      amiArtifactStream.setFilters(filters);
      amiArtifactStream.setUuid(uuid);
      amiArtifactStream.setSourceName(sourceName);
      amiArtifactStream.setAppId(appId);
      amiArtifactStream.setCreatedBy(createdBy);
      amiArtifactStream.setSettingId(settingId);
      amiArtifactStream.setCreatedAt(createdAt);
      amiArtifactStream.setName(name);
      amiArtifactStream.setLastUpdatedBy(lastUpdatedBy);
      amiArtifactStream.setAutoPopulate(autoPopulate);
      amiArtifactStream.setLastUpdatedAt(lastUpdatedAt);
      amiArtifactStream.setServiceId(serviceId);
      amiArtifactStream.setKeywords(keywords);
      amiArtifactStream.setAutoDownload(autoDownload);
      amiArtifactStream.setAutoApproveForProduction(autoApproveForProduction);
      amiArtifactStream.setEntityYamlPath(entityYamlPath);
      amiArtifactStream.setMetadataOnly(metadataOnly);
      return amiArtifactStream;
    }
  }
}
