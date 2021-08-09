package io.harness.delegate.beans.aws.codecommit;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsCodeCommitDataObtainmentParams implements AwsCodeCommitApiParams {
  String triggerUserArn;
  String repoArn;
  List<String> commitIds;
}
