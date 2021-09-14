/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GitConfig;
import software.wings.delegatetasks.validation.capabilities.GitConnectionCapability;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class GitConnectionCapabilityCheck implements CapabilityCheck {
  @Inject private EncryptionService encryptionService;
  @Inject private GitClient gitClient;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    GitConnectionCapability capability = (GitConnectionCapability) delegateCapability;
    GitConfig gitConfig = capability.getGitConfig();
    List<EncryptedDataDetail> encryptedDataDetails = capability.getEncryptedDataDetails();
    try {
      encryptionService.decrypt(gitConfig, encryptedDataDetails, false);
    } catch (Exception e) {
      log.info("Failed to decrypt " + capability.getGitConfig(), e);
      return CapabilityResponse.builder().delegateCapability(capability).validated(false).build();
    }
    gitConfig.setSshSettingAttribute(capability.getSettingAttribute());
    if (isNotEmpty(gitClient.validate(gitConfig))) {
      return CapabilityResponse.builder().delegateCapability(capability).validated(false).build();
    }
    return CapabilityResponse.builder().delegateCapability(capability).validated(true).build();
  }
}
