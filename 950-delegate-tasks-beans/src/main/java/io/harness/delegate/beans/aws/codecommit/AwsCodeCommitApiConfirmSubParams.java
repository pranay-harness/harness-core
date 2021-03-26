package io.harness.delegate.beans.aws.codecommit;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AwsCodeCommitApiConfirmSubParams implements AwsCodeCommitApiParams {
  String topicArn;
  String subscriptionConfirmationMessage;
}
