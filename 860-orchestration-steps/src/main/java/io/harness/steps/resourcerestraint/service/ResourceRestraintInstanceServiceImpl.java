/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.distribution.constraint.Consumer.State.FINISHED;
import static io.harness.pms.contracts.execution.Status.DISCONTINUING;

import static java.util.stream.Collectors.toList;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.ConstraintId;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.distribution.constraint.RunnableConsumers;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.persistence.HPersistence;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.utils.PmsConstants;
import io.harness.repositories.ResourceRestraintInstanceRepository;
import io.harness.steps.resourcerestraint.beans.ResourceRestraint;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance.ResourceRestraintInstanceKeys;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(PIPELINE)
@Slf4j
public class ResourceRestraintInstanceServiceImpl implements ResourceRestraintInstanceService {
  @Inject private ResourceRestraintInstanceRepository restraintInstanceRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private ResourceRestraintRegistry resourceRestraintRegistry;
  @Inject private ResourceRestraintService resourceRestraintService;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  public ResourceRestraintInstance save(ResourceRestraintInstance resourceRestraintInstance) {
    return HPersistence.retry(() -> restraintInstanceRepository.save(resourceRestraintInstance));
  }

  @Override
  public ResourceRestraintInstance activateBlockedInstance(String uuid, String resourceUnit) {
    ResourceRestraintInstance instance =
        restraintInstanceRepository
            .findByUuidAndResourceUnitAndStateIn(uuid, resourceUnit, Collections.singletonList(BLOCKED))
            .orElseThrow(
                () -> new InvalidRequestException("Cannot find ResourceRestraintInstance with id [" + uuid + "]."));

    instance.setState(ACTIVE);
    instance.setAcquireAt(System.currentTimeMillis());

    return save(instance);
  }

  @Override
  public ResourceRestraintInstance finishInstance(String uuid, String resourceUnit) {
    ResourceRestraintInstance instance =
        restraintInstanceRepository
            .findByUuidAndResourceUnitAndStateIn(uuid, resourceUnit, Lists.newArrayList(ACTIVE, BLOCKED))
            .orElseThrow(
                () -> new InvalidRequestException("Cannot find ResourceRestraintInstance with id [" + uuid + "]."));

    instance.setState(FINISHED);
    return save(instance);
  }

  @Override
  public boolean updateActiveConstraintsForInstance(ResourceRestraintInstance instance) {
    boolean isConstraint = false;
    boolean finished;
    String releaseEntityId = instance.getReleaseEntityId();

    if (PmsConstants.RELEASE_ENTITY_TYPE_PLAN.equals(instance.getReleaseEntityType())) {
      try {
        PlanExecution planExecution = planExecutionService.get(releaseEntityId);
        finished = planExecution != null && StatusUtils.finalStatuses().contains(planExecution.getStatus());
      } catch (InvalidRequestException e) {
        log.error("", e);
        return false;
      }
    } else {
      try {
        NodeExecution nodeExecution = nodeExecutionService.getByPlanNodeUuid(
            ResourceRestraintInstanceService.getSetupNodeIdFromReleaseEntityId(releaseEntityId),
            ResourceRestraintInstanceService.getPlanExecutionIdFromReleaseEntityId(releaseEntityId));
        finished = nodeExecution != null
            && (StatusUtils.finalStatuses().contains(nodeExecution.getStatus())
                || DISCONTINUING == nodeExecution.getStatus());
      } catch (InvalidRequestException e) {
        log.error("", e);
        return false;
      }
    }

    if (finished) {
      if (resourceRestraintRegistry.consumerFinished(new ConstraintId(instance.getResourceRestraintId()),
              new ConstraintUnit(instance.getResourceUnit()), new ConsumerId(instance.getUuid()), ImmutableMap.of())) {
        isConstraint = true;
      }
    }
    return isConstraint;
  }

  @Override
  public void updateBlockedConstraints(Set<String> constraints) {
    List<? extends ResourceRestraint> restraintList = resourceRestraintService.getConstraintsIn(constraints);
    for (ResourceRestraint restraint : restraintList) {
      final Constraint constraint = createAbstraction(restraint);
      final List<ConstraintUnit> units = units(restraint);
      if (isEmpty(units)) {
        continue;
      }

      log.info("Resource constraint {} has running units {}", restraint.getUuid(), Joiner.on(", ").join(units));

      units.forEach(unit -> {
        final RunnableConsumers runnableConsumers = constraint.runnableConsumers(unit, resourceRestraintRegistry);
        for (ConsumerId consumerId : runnableConsumers.getConsumerIds()) {
          if (!constraint.consumerUnblocked(unit, consumerId, null, resourceRestraintRegistry)) {
            break;
          }
        }
      });
    }
  }

  @Override
  public List<ResourceRestraintInstance> getAllByRestraintIdAndResourceUnitAndStates(
      String resourceRestraintId, String resourceUnit, List<Consumer.State> states) {
    Query query = query(
        new Criteria().andOperator(where(ResourceRestraintInstanceKeys.resourceRestraintId).is(resourceRestraintId),
            where(ResourceRestraintInstanceKeys.resourceUnit).is(resourceUnit),
            where(ResourceRestraintInstanceKeys.state).in(BLOCKED, ACTIVE)));

    return mongoTemplate.find(query, ResourceRestraintInstance.class);
  }

  @Override
  public Constraint createAbstraction(ResourceRestraint resourceRestraint) {
    return Constraint.builder()
        .id(new ConstraintId(resourceRestraint.getUuid()))
        .spec(Constraint.Spec.builder()
                  .limits(resourceRestraint.getCapacity())
                  .strategy(resourceRestraint.getStrategy())
                  .build())
        .build();
  }

  @Override
  public int getMaxOrder(String resourceRestraintId) {
    Optional<ResourceRestraintInstance> instance =
        restraintInstanceRepository.findFirstByResourceRestraintIdOrderByOrderDesc(resourceRestraintId);

    return instance.map(ResourceRestraintInstance::getOrder).orElse(0);
  }

  @Override
  public int getAllCurrentlyAcquiredPermits(String scope, String releaseEntityId) {
    int currentPermits = 0;

    List<ResourceRestraintInstance> instances =
        restraintInstanceRepository.findByReleaseEntityTypeAndReleaseEntityId(scope, releaseEntityId);
    if (isNotEmpty(instances)) {
      for (ResourceRestraintInstance instance : instances) {
        currentPermits += instance.getPermits();
      }
    }

    return currentPermits;
  }

  private List<ConstraintUnit> units(ResourceRestraint constraint) {
    Set<String> units = new HashSet<>();

    Query query = query(
        new Criteria().andOperator(where(ResourceRestraintInstanceKeys.resourceRestraintId).is(constraint.getUuid()),
            where(ResourceRestraintInstanceKeys.state).in(BLOCKED, ACTIVE)));

    List<ResourceRestraintInstance> instances = mongoTemplate.find(query, ResourceRestraintInstance.class);
    for (ResourceRestraintInstance instance : instances) {
      units.add(instance.getResourceUnit());
    }

    return units.stream().map(ConstraintUnit::new).collect(toList());
  }
}
