/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.resourcegroup.reconciliation;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.resourcegroup.framework.remote.mapper.ResourceGroupMapper;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.framework.service.impl.ResourceGroupValidatorServiceImpl;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class ResourceGroupAsyncReconciliationHandler implements MongoPersistenceIterator.Handler<ResourceGroup> {
  public static final String GROUP = "RESOURCE_GROUP_ASYNC_RECONCILIATION";

  @Inject private final PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private final MongoTemplate mongoTemplate;
  @Inject private final ResourceGroupService resourceGroupService;
  @Inject private final ResourceGroupValidatorServiceImpl resourceGroupValidatorService;

  @Override
  public void handle(ResourceGroup resourceGroup) {
    boolean updated = resourceGroupValidatorService.sanitizeResourceSelectors(resourceGroup);
    if (updated) {
      resourceGroupService.update(ResourceGroupMapper.toDTO(resourceGroup), false);
    }
  }

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("ResourceGroupReconciliationIterator")
            .poolSize(3)
            .interval(ofMinutes(1))
            .build(),
        ResourceGroup.class,
        MongoPersistenceIterator.<ResourceGroup, SpringFilterExpander>builder()
            .clazz(ResourceGroup.class)
            .fieldName(ResourceGroupKeys.nextIteration)
            .targetInterval(ofHours(1))
            .acceptableNoAlertDelay(ofHours(1))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }
}
