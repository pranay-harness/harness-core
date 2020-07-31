package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.format;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintId;
import io.harness.distribution.constraint.ConstraintRegistry;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.distribution.constraint.UnableToLoadConstraintException;
import io.harness.distribution.constraint.UnableToRegisterConsumerException;
import io.harness.distribution.constraint.UnableToSaveConstraintException;
import io.harness.exception.InvalidRequestException;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance.ResourceRestraintInstanceBuilder;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance.ResourceRestraintInstanceKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class ResourceRestraintRegistryImpl implements ConstraintRegistry {
  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private ResourceRestraintInstanceRepository resourceRestraintInstanceRepository;

  public ConstraintRegistry getRegistry() {
    return this;
  }

  @Override
  public void save(ConstraintId id, Constraint.Spec spec) throws UnableToSaveConstraintException {
    // to be implemented
  }

  @Override
  public Constraint load(ConstraintId id) throws UnableToLoadConstraintException {
    final ResourceRestraint resourceRestraint = resourceRestraintService.get(null, id.getValue());
    return createAbstraction(resourceRestraint);
  }

  @Override
  public List<Consumer> loadConsumers(ConstraintId id, ConstraintUnit unit) {
    List<Consumer> consumers = new ArrayList<>();

    List<ResourceRestraintInstance> instances =
        resourceRestraintInstanceRepository.findByResourceUnitOrderByOrderAsc(unit.getValue());

    instances.forEach(instance
        -> consumers.add(Consumer.builder()
                             .id(new ConsumerId(instance.getUuid()))
                             .state(instance.getState())
                             .permits(instance.getPermits())
                             .context(ImmutableMap.of(ResourceRestraintInstanceKeys.releaseEntityType,
                                 instance.getReleaseEntityType(), ResourceRestraintInstanceKeys.releaseEntityId,
                                 instance.getReleaseEntityId()))
                             .build()));
    return consumers;
  }

  @Override
  public boolean registerConsumer(ConstraintId id, ConstraintUnit unit, Consumer consumer, int currentlyRunning)
      throws UnableToRegisterConsumerException {
    ResourceRestraint resourceRestraint = resourceRestraintService.get(null, id.getValue());
    if (resourceRestraint == null) {
      throw new InvalidRequestException(format("There is no resource constraint with id: %s", id.getValue()));
    }

    final ResourceRestraintInstanceBuilder builder =
        ResourceRestraintInstance.builder()
            .uuid(consumer.getId().getValue())
            .resourceRestraintId(id.getValue())
            .resourceUnit(unit.getValue())
            .releaseEntityType((String) consumer.getContext().get(ResourceRestraintInstanceKeys.releaseEntityType))
            .releaseEntityId((String) consumer.getContext().get(ResourceRestraintInstanceKeys.releaseEntityId))
            .permits(consumer.getPermits())
            .state(consumer.getState())
            .order((int) consumer.getContext().get(ResourceRestraintInstanceKeys.order));

    if (Consumer.State.ACTIVE == consumer.getState()) {
      builder.acquireAt(System.currentTimeMillis());
    }

    try {
      resourceRestraintInstanceRepository.save(builder.build());
    } catch (DuplicateKeyException e) {
      logger.info("Failed to add ResourceRestraintInstance", e);
      return false;
    }

    return true;
  }

  @Override
  public boolean adjustRegisterConsumerContext(ConstraintId id, Map<String, Object> context) {
    // to be implemented
    return false;
  }

  @Override
  public boolean consumerUnblocked(
      ConstraintId id, ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context) {
    // to be implemented
    return false;
  }

  @Override
  public boolean consumerFinished(
      ConstraintId id, ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context) {
    // to be implemented
    return false;
  }

  @Override
  public boolean overlappingScope(Consumer consumer, Consumer blockedConsumer) {
    // to be implemented
    return false;
  }

  private Constraint createAbstraction(ResourceRestraint resourceRestraint) {
    return Constraint.builder()
        .id(new ConstraintId(resourceRestraint.getUuid()))
        .spec(Constraint.Spec.builder()
                  .limits(resourceRestraint.getCapacity())
                  .strategy(resourceRestraint.getStrategy())
                  .build())
        .build();
  }
}
