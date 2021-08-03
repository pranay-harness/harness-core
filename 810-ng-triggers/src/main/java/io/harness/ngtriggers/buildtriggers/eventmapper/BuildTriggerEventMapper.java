package io.harness.ngtriggers.buildtriggers.eventmapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.contracts.PollingResponse;
import io.harness.repositories.spring.NGTriggerRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.PIPELINE)
public class BuildTriggerEventMapper {
  private final NGTriggerRepository ngTriggerRepository;

  public void consumeBuildTriggerEvent(PollingResponse pollingResponse) {
    int signaturesCount = pollingResponse.getSignaturesCount();
    if (signaturesCount > 0) {
      String accountId = pollingResponse.getAccountId();
      for (int i = 0; i < signaturesCount; i++) {
        String signature = pollingResponse.getSignatures(i);
        ngTriggerRepository.findByAccountIdAndSignatureAndEnabledAndDeletedNot(accountId, signature, true, true);
      }
    }
  }
}
