package software.wings.beans.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.artifact.ArtifactStreamType.AMI;

import com.google.common.base.Joiner;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.NameValuePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@JsonTypeName("AMI")
@Data
@EqualsAndHashCode(callSuper = true)
public class AmiArtifactStream extends ArtifactStream {
  private String region;
  private String platform;
  private List<Tag> tags;
  private List<FilterClass> filters;

  public AmiArtifactStream() {
    super(AMI.name());
    super.setMetadataOnly(true);
  }

  @Builder
  public AmiArtifactStream(String uuid, String appId, EmbeddedUser createdBy, long createdAt,
      EmbeddedUser lastUpdatedBy, long lastUpdatedAt, String entityYamlPath, String sourceName, String settingId,
      String name, boolean autoPopulate, String serviceId, String region, String platform, List<Tag> tags,
      List<FilterClass> filters, String accountId, Set<String> keywords, boolean sample) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath, AMI.name(), sourceName,
        settingId, name, autoPopulate, serviceId, true, accountId, keywords, sample);
    this.region = region;
    this.platform = platform;
    this.tags = tags;
    this.filters = filters;
  }

  @Override
  public String fetchArtifactDisplayName(String amiName) {
    return isEmpty(tags) ? format("%s_%s", getRegion(), amiName) : format("%s_%s", getSourceName(), amiName);
  }

  @Override
  public String generateSourceName() {
    if (isEmpty(tags) && isEmpty(filters)) {
      return region;
    }
    List<String> tagFields = new ArrayList<>();
    if (tags != null) {
      tagFields = tags.stream().map(tag -> tag.getKey() + ":" + tag.getValue()).collect(Collectors.toList());
    }

    List<String> filterFields = new ArrayList<>();
    if (filters != null) {
      filterFields = filters.stream()
                         .map(filterClass -> filterClass.getKey() + ":" + filterClass.getValue())
                         .collect(Collectors.toList());
    }

    return region + (tagFields.size() > 0 ? ":" + Joiner.on("_").join(tagFields) : "")
        + (filterFields.size() > 0 ? ":" + Joiner.on("_").join(filterFields) : "");
  }

  @Override
  public ArtifactStreamAttributes fetchArtifactStreamAttributes() {
    Map<String, List<String>> tagMap = new HashMap<>();
    Map<String, String> filterMap = new HashMap<>();
    if (tags != null) {
      Map<String, List<Tag>> collect = tags.stream().collect(Collectors.groupingBy(Tag::getKey));
      tagMap = tags.stream()
                   .collect(Collectors.groupingBy(Tag::getKey))
                   .keySet()
                   .stream()
                   .collect(Collectors.toMap(
                       identity(), s -> collect.get(s).stream().map(tag -> tag.value).collect(toList()), (a, b) -> b));
    }

    if (filters != null) {
      filterMap = filters.stream().collect(Collectors.toMap(FilterClass::getKey, FilterClass::getValue, (a, b) -> b));
    }

    return ArtifactStreamAttributes.builder()
        .artifactStreamType(getArtifactStreamType())
        .region(region)
        .tags(tagMap)
        .filters(filterMap)
        .platform(platform)
        .build();
  }

  @Data
  public static class FilterClass {
    private String key;
    private String value;
  }

  @Data
  public static class Tag {
    private String key;
    private String value;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends ArtifactStream.Yaml {
    private String platform;
    private String region;
    private List<NameValuePair.Yaml> amiTags = new ArrayList<>();
    private List<NameValuePair.Yaml> amiFilters = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, String serverName, List<NameValuePair.Yaml> amiTags, String region,
        String platform, List<NameValuePair.Yaml> amiFilters) {
      super(AMI.name(), harnessApiVersion, serverName);
      this.amiTags = amiTags;
      this.region = region;
      this.platform = platform;
      this.amiFilters = amiFilters;
    }
  }
}
