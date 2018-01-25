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
import software.wings.beans.NameValuePair;
import software.wings.utils.Util;

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
}
