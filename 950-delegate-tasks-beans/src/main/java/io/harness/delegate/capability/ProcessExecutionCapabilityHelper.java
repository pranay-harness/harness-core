/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.capability;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ProcessExecutionCapabilityHelper {
  public static final String TERRAFORM = "terraform";
  public static final String TERRAGRUNT = "terragrunt";

  public static List<ExecutionCapability> generateExecutionCapabilitiesForProcessExecutor(String category,
      List<String> processExecutorArguments, List<EncryptedDataDetail> encryptedDataDetails,
      ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.add(
        ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(category, processExecutorArguments));

    if (isNotEmpty(encryptedDataDetails)) {
      List<ExecutionCapability> capabilitiesForEncryption =
          EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
              encryptedDataDetails, maskingEvaluator);
      if (isNotEmpty(capabilitiesForEncryption)) {
        executionCapabilities.addAll(capabilitiesForEncryption);
      }
    }
    return executionCapabilities;
  }

  public static List<ExecutionCapability> generateExecutionCapabilitiesForTerraform(
      List<EncryptedDataDetail> encryptedDataDetails, ExpressionEvaluator maskingEvaluator) {
    List<String> processExecutorArguments = new ArrayList<>();
    processExecutorArguments.add("/bin/sh");
    processExecutorArguments.add("-c");
    processExecutorArguments.add("terraform --version");

    return generateExecutionCapabilitiesForProcessExecutor(
        TERRAFORM, processExecutorArguments, encryptedDataDetails, maskingEvaluator);
  }

  public static List<ExecutionCapability> generateExecutionCapabilitiesForTerragrunt(
      List<EncryptedDataDetail> encryptedDataDetails, ExpressionEvaluator maskingEvaluator) {
    List<String> processExecutorArguments = new ArrayList<>();
    processExecutorArguments.add("/bin/sh");
    processExecutorArguments.add("-c");
    processExecutorArguments.add("terragrunt --version");

    return generateExecutionCapabilitiesForProcessExecutor(
        TERRAGRUNT, processExecutorArguments, encryptedDataDetails, maskingEvaluator);
  }
}
