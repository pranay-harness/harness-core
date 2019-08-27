package io.harness.governance.pipeline.model;

import io.harness.data.structure.CollectionUtils;
import lombok.Value;

import java.util.List;
import javax.annotation.Nonnull;

/**
 * Associates a weight with a set of tags.
 */
@Value
public class PipelineGovernanceRule {
  private List<Tag> tags;

  @Nonnull private MatchType matchType;
  private int weight;
  private String note;
  private boolean distributedEqually;

  public List<Tag> getTags() {
    return CollectionUtils.emptyIfNull(tags);
  }
}
