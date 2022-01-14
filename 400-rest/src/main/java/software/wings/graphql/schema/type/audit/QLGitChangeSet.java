package software.wings.graphql.schema.type.audit;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLChangeSetKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLGitChangeSet implements QLChangeSet {
  private String id;
  private List<QLChangeDetails> changes;
  private Long triggeredAt;
  private QLRequestInfo request;
  private String failureStatusMsg;
  private String author;
  private String gitCommitId;
  private String repoUrl;
}
