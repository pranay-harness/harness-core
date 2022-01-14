package io.harness.delegate.beans.aws.codecommit;

import io.harness.beans.CommitDetails;
import io.harness.beans.Repository;
import io.harness.beans.WebhookGitUser;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsCodeCommitDataObtainmentTaskResult implements AwsCodeCommitApiResult {
  WebhookGitUser webhookGitUser;
  List<CommitDetails> commitDetailsList;
  Repository repository;
}
