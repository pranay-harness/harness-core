package io.harness.resourcegroup.reconciliation;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.resourcegroup.framework.service.ResourceGroupService;
import io.harness.resourcegroup.model.ResourceGroup;
import io.harness.resourcegroup.model.ResourceGroup.ResourceGroupKeys;

import com.google.inject.Inject;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class ResourceGroupAsyncReconciliationHandler implements MongoPersistenceIterator.Handler<ResourceGroup> {
  public static final String GROUP = "RESOURCE_GROUP_ASYNC_RECONCILIATION";

  @Inject private final PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject MongoTemplate mongoTemplate;
  @Inject private final ResourceGroupService resourceGroupService;

  @Override
  public void handle(ResourceGroup resourceGroup) {
    resourceGroupService.deleteStaleResources(resourceGroup);
  }

  public void registerIterators() {
    Duration interval = ofMinutes(15);
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("ResourceGroupAsyncReconciliation")
            .poolSize(5)
            .interval(interval)
            .build(),
        ResourceGroup.class,
        MongoPersistenceIterator.<ResourceGroup, MorphiaFilterExpander<ResourceGroup>>builder()
            .clazz(ResourceGroup.class)
            .fieldName(ResourceGroupKeys.nextIteration)
            .targetInterval(interval)
            .acceptableNoAlertDelay(ofMinutes(60))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider(mongoTemplate))
            .redistribute(true));
  }
}