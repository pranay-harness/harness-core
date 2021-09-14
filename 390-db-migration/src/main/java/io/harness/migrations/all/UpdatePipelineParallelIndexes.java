/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ExecutionContext;
import io.harness.logging.ExceptionLogger;
import io.harness.migrations.Migration;
import io.harness.persistence.HIterator;

import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.PipelineServiceImpl;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdatePipelineParallelIndexes implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    try (HIterator<Application> iterator =
             new HIterator<>(wingsPersistence.createQuery(Application.class, excludeAuthority).fetch())) {
      for (Application application : iterator) {
        migrate(application);
      }
    }
  }

  public void migrate(Application application) {
    try (HIterator<Pipeline> iterator = new HIterator<>(
             wingsPersistence.createQuery(Pipeline.class).filter(PipelineKeys.appId, application.getUuid()).fetch())) {
      for (Pipeline pipeline : iterator) {
        migrate(pipeline);
      }
    }
  }

  public void migrate(Pipeline pipeline) {
    try {
      PipelineServiceImpl.ensurePipelineStageUuidAndParallelIndex(pipeline);
      wingsPersistence.save(pipeline);
    } catch (WingsException exception) {
      exception.addContext(Pipeline.class, pipeline.getUuid());
      ExceptionLogger.logProcessedMessages(exception, ExecutionContext.MANAGER, log);
    } catch (RuntimeException exception) {
      log.error("", exception);
    }
  }
}
