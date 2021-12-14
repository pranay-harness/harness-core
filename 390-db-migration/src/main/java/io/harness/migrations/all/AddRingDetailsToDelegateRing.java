package io.harness.migrations.all;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateRing;
import io.harness.delegate.utils.DelegateRingConstants;
import io.harness.migrations.Migration;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class AddRingDetailsToDelegateRing implements Migration {
  @Inject private HPersistence persistence;
  private static final String DELEGATE_IMAGE_TAG = "harness/delegate:latest";
  private static final String UPGRADER_IMAGE_TAG = "harness/upgrader:latest";

  @Override
  public void migrate() {
    log.info("Starting the migration for adding ring details in delegateRing collection.");

    persistence.save(delegateRing(DelegateRingConstants.RING_NAME_1));
    log.info("Added ring1 details.");
    persistence.save(delegateRing(DelegateRingConstants.RING_NAME_2));
    log.info("Added ring2 details.");
    persistence.save(delegateRing(DelegateRingConstants.RING_NAME_3));
    log.info("Added ring3 details.");

    log.info("Migration complete for adding ring details in delegateRing collection.");
  }

  private DelegateRing delegateRing(String ringName) {
    return DelegateRing.builder()
        .ringName(ringName)
        .delegateImageTag(DELEGATE_IMAGE_TAG)
        .upgraderImageTag(UPGRADER_IMAGE_TAG)
        .build();
  }
}
