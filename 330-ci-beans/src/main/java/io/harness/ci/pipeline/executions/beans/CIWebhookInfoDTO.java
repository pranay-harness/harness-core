package io.harness.ci.pipeline.executions.beans;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.build.PublishedArtifact;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.ci.CIWebhookInfoProto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.CI)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_EMPTY)
public class CIWebhookInfoDTO {
  private String event;
  private CIBuildAuthor author;
  private CIBuildBranchHook branch;
  private CIBuildPRHook pullRequest;
  private List<PublishedArtifact> publishedArtifacts;

  public CIWebhookInfoProto toProto() {
    CIWebhookInfoProto.Builder builder = CIWebhookInfoProto.newBuilder();
    if (event != null) {
      builder.setEvent(event);
    }
    if (author != null) {
      builder.setAuthor(author.toProto());
    }
    if (branch != null) {
      builder.setBranch(branch.toProto());
    }
    if (pullRequest != null) {
      builder.setPullRequest(pullRequest.toProto());
    }
    if (EmptyPredicate.isNotEmpty(publishedArtifacts)) {
      builder.addAllPublishedArtifacts(publishedArtifacts.stream()
                                           .filter(Objects::nonNull)
                                           .map(PublishedArtifact::toProto)
                                           .collect(Collectors.toList()));
    }
    return builder.build();
  }
}
