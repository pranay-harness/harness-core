package io.harness.governance.pipeline.model;

import io.harness.data.structure.CollectionUtils;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Associates a weight with a set of tags.
 */
@Value
public class PipelineGovernanceRule {
  private List<Tag> tags;

  @Nonnull private MatchType matchType;
  private int weight;
  @Nullable private String note;

  public List<Tag> getTags() {
    List<Tag> tags = CollectionUtils.emptyIfNull(this.tags);
    return Collections.unmodifiableList(tags);
  }
}
