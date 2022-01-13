/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(PIPELINE)
public class NGPipelineCommonsModule extends AbstractModule {
  private static final AtomicReference<NGPipelineCommonsModule> instanceRef = new AtomicReference<>();

  public static NGPipelineCommonsModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGPipelineCommonsModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    bind(ExecutorService.class)
        .annotatedWith(Names.named("NgPipelineCommonsExecutor"))
        .toInstance(ThreadPool.create(1, 1, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-ng-pipeline-commons-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));
  }
}
