package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitSuccessFulChangeDetail implements SuccessfulChangeDetail {
  String commitId;
  String processingCommitId;
}
