package io.harness.delegate.task.executioncapability;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.delegate.beans.connector.gitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.git.NGGitService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GitConnectionNGCapabilityChecker implements CapabilityCheck {
  @Inject private SecretDecryptionService decryptionService;
  @Inject private NGGitService gitService;

  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    GitConnectionNGCapability capability = (GitConnectionNGCapability) delegateCapability;
    GitConfigDTO gitConfig = capability.getGitConfig();
    List<EncryptedDataDetail> encryptedDataDetails = capability.getEncryptedDataDetails();
    try {
      decryptionService.decrypt(gitConfig.getGitAuth(), encryptedDataDetails);
    } catch (Exception e) {
      logger.info("Failed to decrypt " + capability.getGitConfig(), e);
      return CapabilityResponse.builder().delegateCapability(capability).validated(false).build();
    }
    // todo @deepak: see how we can account ID here. connector ID and account Id is to be brought in view of delegate
    // tasks and causing issues.
    if (isNotEmpty(gitService.validate(gitConfig, "accountId"))) {
      return CapabilityResponse.builder().delegateCapability(capability).validated(false).build();
    }
    return CapabilityResponse.builder().delegateCapability(capability).validated(true).build();
  }
}
