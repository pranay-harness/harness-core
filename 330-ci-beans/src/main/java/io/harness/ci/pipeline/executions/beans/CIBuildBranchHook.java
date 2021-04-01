package io.harness.ci.pipeline.executions.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ci.CIBuildBranchHookProto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CI)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIBuildBranchHook {
  private String name;
  private String link;
  private String state;
  private List<CIBuildCommit> commits;

  public CIBuildBranchHookProto toProto() {
    CIBuildBranchHookProto.Builder builder = CIBuildBranchHookProto.newBuilder();
    if (name != null) {
      builder.setName(name);
    }
    if (link != null) {
      builder.setLink(link);
    }
    if (state != null) {
      builder.setState(state);
    }
    if (EmptyPredicate.isNotEmpty(commits)) {
      builder.addAllCommits(
          commits.stream().filter(Objects::nonNull).map(CIBuildCommit::toProto).collect(Collectors.toList()));
    }
    return builder.build();
  }
}
