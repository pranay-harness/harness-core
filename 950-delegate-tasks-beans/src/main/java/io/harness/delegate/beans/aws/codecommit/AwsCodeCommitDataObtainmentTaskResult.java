/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
