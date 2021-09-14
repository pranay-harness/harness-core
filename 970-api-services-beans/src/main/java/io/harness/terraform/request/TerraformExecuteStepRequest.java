/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.terraform.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.LogCallback;
import io.harness.logging.PlanJsonLogOutputStream;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@OwnedBy(CDP)
public class TerraformExecuteStepRequest {
  String tfBackendConfigsFile;
  String tfOutputsFile;
  List<String> tfVarFilePaths;
  String varParams; // Needed to send inline variable values in CG
  String uiLogs; // Needed to NOT log secrets in CG
  @Nonnull @NotEmpty String scriptDirectory;
  Map<String, String> envVars;
  List<String> targets;
  String workspace;
  boolean isRunPlanOnly; // Needed to send inline variable values in CG
  EncryptedRecordData encryptedTfPlan;
  EncryptionConfig encryptionConfig;
  @Builder.Default boolean isSkipRefreshBeforeApplyingPlan = true;
  boolean isSaveTerraformJson;
  @Nonnull LogCallback logCallback;
  @Nonnull PlanJsonLogOutputStream planJsonLogOutputStream;
  long timeoutInMillis;
  boolean isTfPlanDestroy;
}
