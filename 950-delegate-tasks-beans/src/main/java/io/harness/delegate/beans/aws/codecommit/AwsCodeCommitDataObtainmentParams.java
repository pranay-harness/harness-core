/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
