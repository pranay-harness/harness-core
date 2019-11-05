package io.harness.governance.pipeline.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.data.structure.CollectionUtils;
import lombok.Value;

import java.util.List;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class Restriction {
  public enum RestrictionType { APP_BASED, TAG_BASED }

  private RestrictionType type;
  private List<String> appIds;
  private List<Tag> tags;

  public List<String> getAppIds() {
    return CollectionUtils.emptyIfNull(appIds);
  }

  public List<Tag> getTags() {
    return CollectionUtils.emptyIfNull(tags);
  }
}
