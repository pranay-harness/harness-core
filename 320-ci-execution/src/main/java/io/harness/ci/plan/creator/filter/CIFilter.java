package io.harness.ci.plan.creator.filter;

import io.harness.pms.pipeline.filter.PipelineFilter;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CIFilter implements PipelineFilter {
  @Singular Set<String> repoNames;

  public void addRepoNames(Set<String> repoNames) {
    if (this.repoNames == null) {
      this.repoNames = new HashSet<>();
    } else if (!(this.repoNames instanceof HashSet)) {
      this.repoNames = new HashSet<>(this.repoNames);
    }

    this.repoNames.addAll(repoNames);
  }
}
