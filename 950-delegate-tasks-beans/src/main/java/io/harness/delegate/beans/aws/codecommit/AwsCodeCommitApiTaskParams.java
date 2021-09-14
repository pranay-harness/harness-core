/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.aws.codecommit;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
public class AwsCodeCommitApiTaskParams
    extends ConnectorCapabilityBaseHelper implements ExecutionCapabilityDemander, TaskParameters {
  static final String AWS_URL = "https://aws.amazon.com/";

  AwsCodeCommitApiParams apiParams;

  AwsCodeCommitConnectorDTO awsCodeCommitConnectorDTO;
  List<EncryptedDataDetail> encryptedDataDetails;

  @NotEmpty AwsCodeCommitRequestType requestType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    capabilityList.add(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL, maskingEvaluator));
    if (awsCodeCommitConnectorDTO != null) {
      populateDelegateSelectorCapability(capabilityList, awsCodeCommitConnectorDTO.getDelegateSelectors());
    }
    return capabilityList;
  }
}
