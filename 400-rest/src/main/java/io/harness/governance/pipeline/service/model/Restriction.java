package io.harness.governance.pipeline.service.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
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
