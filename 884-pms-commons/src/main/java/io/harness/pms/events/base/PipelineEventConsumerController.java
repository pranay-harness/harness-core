/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.events.base;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineEventConsumerController implements Managed {
  private ExecutorService executorService =
      Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("pipeline-event-consumer-%d").build());
  private List<PmsRedisConsumer> redisConsumers = new ArrayList<>();

  public void register(PmsRedisConsumer consumer, int threads) {
    IntStream.rangeClosed(1, threads).forEach(value -> {
      redisConsumers.add(consumer);
      executorService.submit(consumer);
    });
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    // Do nothing
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    redisConsumers.forEach(PmsRedisConsumer::shutDown);
    executorService.shutdownNow();
    executorService.awaitTermination(1, TimeUnit.HOURS);
  }
}
