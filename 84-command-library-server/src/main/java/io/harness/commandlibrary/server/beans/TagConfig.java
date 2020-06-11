package io.harness.commandlibrary.server.beans;

import static org.apache.commons.collections4.SetUtils.emptyIfNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.commandlibrary.server.utils.JsonSerializable;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.elasticsearch.common.util.set.Sets;

import java.util.Set;

@Value
@FieldNameConstants(innerTypeName = "TagConfigKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagConfig implements JsonSerializable {
  Set<String> allowedTags;
  Set<String> importantTags;

  @Builder
  public TagConfig(@JsonProperty(TagConfigKeys.allowedTags) Set<String> allowedTags,
      @JsonProperty(TagConfigKeys.importantTags) Set<String> importantTags) {
    this.importantTags = emptyIfNull(importantTags);
    this.allowedTags = Sets.union(emptyIfNull(allowedTags), emptyIfNull(importantTags));
  }
}
