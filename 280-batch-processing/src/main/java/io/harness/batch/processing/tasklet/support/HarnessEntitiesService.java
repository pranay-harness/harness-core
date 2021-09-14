/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.tasklet.support;

import io.harness.exception.InvalidRequestException;

import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HarnessEntitiesService {
  public enum HarnessEntities { APP, SERVICE, ENV }

  @Value
  public static class CacheKey {
    public String entityId;
    public HarnessEntities entity;
  }

  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;

  public String fetchEntityName(HarnessEntities entity, String entityId) {
    switch (entity) {
      case APP:
        return cloudToHarnessMappingService.getApplicationName(entityId);
      case ENV:
        return cloudToHarnessMappingService.getEnvironmentName(entityId);
      case SERVICE:
        return cloudToHarnessMappingService.getServiceName(entityId);
      default:
        throw new InvalidRequestException("Invalid EntityType " + entity);
    }
  }
}
