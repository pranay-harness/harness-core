package io.harness.ci.pipeline.executions.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ci.CIBuildPRHookProto;

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
public class CIBuildPRHook {
  private Long id;
  private String link;
  private String title;
  private String body;
  private String sourceRepo;
  private String sourceBranch;
  private String targetBranch;
  private String state;
  private List<CIBuildCommit> commits;

  public CIBuildPRHookProto toProto() {
    CIBuildPRHookProto.Builder builder = CIBuildPRHookProto.newBuilder();
    if (id != null) {
      builder.setId(id);
    }
    if (link != null) {
      builder.setLink(link);
    }
    if (title != null) {
      builder.setTitle(title);
    }
    if (body != null) {
      builder.setBody(body);
    }
    if (sourceRepo != null) {
      builder.setSourceRepo(sourceRepo);
    }
    if (sourceBranch != null) {
      builder.setSourceBranch(sourceBranch);
    }
    if (targetBranch != null) {
      builder.setTargetBranch(targetBranch);
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
