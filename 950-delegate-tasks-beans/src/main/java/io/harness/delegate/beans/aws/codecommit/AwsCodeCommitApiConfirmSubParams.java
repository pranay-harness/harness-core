/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.aws.codecommit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsCodeCommitApiConfirmSubParams implements AwsCodeCommitApiParams {
  String topicArn;
  String subscriptionConfirmationMessage;
}
