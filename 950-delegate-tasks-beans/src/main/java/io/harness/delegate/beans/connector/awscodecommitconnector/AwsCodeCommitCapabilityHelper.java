/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.connector.awscodecommitconnector;

import io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsCodeCommitCapabilityHelper extends ConnectorCapabilityBaseHelper {
  private static final String AWS_URL = "https://aws.amazon.com/";

  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ConnectorConfigDTO connectorConfigDTO, ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilityList = new ArrayList<>();
    AwsCodeCommitConnectorDTO awsCodeCommitConnectorDTO = (AwsCodeCommitConnectorDTO) connectorConfigDTO;
    AwsCodeCommitAuthenticationDTO authentication = awsCodeCommitConnectorDTO.getAuthentication();
    if (authentication.getAuthType() == AwsCodeCommitAuthType.HTTPS) {
      capabilityList.add(
          HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL, maskingEvaluator));
    }
    populateDelegateSelectorCapability(capabilityList, awsCodeCommitConnectorDTO.getDelegateSelectors());
    return capabilityList;
  }
}
