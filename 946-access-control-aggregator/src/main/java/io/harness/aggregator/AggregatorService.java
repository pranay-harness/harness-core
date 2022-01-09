/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aggregator;

import static io.harness.aggregator.AggregatorConfiguration.ACCESS_CONTROL_SERVICE;
import static io.harness.aggregator.models.AggregatorSecondarySyncState.SecondarySyncStatus.SECONDARY_SYNC_REQUESTED;
import static io.harness.aggregator.models.AggregatorSecondarySyncState.SecondarySyncStatus.SECONDARY_SYNC_RUNNING;
import static io.harness.aggregator.models.AggregatorSecondarySyncState.SecondarySyncStatus.SWITCH_TO_PRIMARY_REQUESTED;

import io.harness.aggregator.controllers.AggregatorController;
import io.harness.aggregator.models.AggregatorSecondarySyncState;
import io.harness.aggregator.repositories.AggregatorSecondarySyncStateRepository;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import io.dropwizard.lifecycle.Managed;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class AggregatorService implements Managed {
  private final AggregatorController aggregatorController;
  private final ScheduledExecutorService controllerExecutorService;
  private final AggregatorSecondarySyncStateRepository aggregatorSecondarySyncStateRepository;

  @Inject
  public AggregatorService(AggregatorController aggregatorController,
      AggregatorSecondarySyncStateRepository aggregatorSecondarySyncStateRepository) {
    this.aggregatorController = aggregatorController;
    this.aggregatorSecondarySyncStateRepository = aggregatorSecondarySyncStateRepository;
    this.controllerExecutorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("aggregator-controller").build());
  }

  @Override
  public void start() {
    controllerExecutorService.scheduleWithFixedDelay(aggregatorController, 0, 120, TimeUnit.SECONDS);
  }

  public AggregatorSecondarySyncState requestSecondarySync() {
    try {
      return aggregatorSecondarySyncStateRepository.create(AggregatorSecondarySyncState.builder()
                                                               .identifier(ACCESS_CONTROL_SERVICE)
                                                               .secondarySyncStatus(SECONDARY_SYNC_REQUESTED)
                                                               .build());
    } catch (DuplicateKeyException e) {
      throw new InvalidRequestException(
          "A secondary sync is already initiated. Please let the previous lifecycle complete before initiating another secondary sync");
    }
  }

  public AggregatorSecondarySyncState requestSwitchToPrimary() {
    Optional<AggregatorSecondarySyncState> optional =
        aggregatorSecondarySyncStateRepository.findByIdentifier(ACCESS_CONTROL_SERVICE);
    if (optional.isPresent()) {
      if (SECONDARY_SYNC_RUNNING.equals(optional.get().getSecondarySyncStatus())) {
        return aggregatorSecondarySyncStateRepository.updateStatus(optional.get(), SWITCH_TO_PRIMARY_REQUESTED);
      } else {
        throw new InvalidRequestException(
            "The secondary sync state is not in RUNNING state. Cannot switch to primary right now.");
      }
    } else {
      throw new InvalidRequestException("No secondary sync has been requested.");
    }
  }

  @Override
  public void stop() {
    controllerExecutorService.shutdownNow();
    try {
      log.info("Waiting for Aggregator Controller to shutdown gracefully.");
      controllerExecutorService.awaitTermination(1, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Aggregator Controller did not shutdown gracefully. Exiting", e);
    }
  }
}
