package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.allChecks;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.sm.states.ResourceConstraintState.HoldingScope.WORKFLOW;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.Constraint.Spec;
import io.harness.distribution.constraint.ConstraintId;
import io.harness.distribution.constraint.ConstraintRegistry;
import io.harness.distribution.constraint.ConstraintUnit;
import io.harness.distribution.constraint.Consumer;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.distribution.constraint.ConsumerId;
import io.harness.distribution.constraint.RunnableConsumers;
import io.harness.distribution.constraint.UnableToLoadConstraintException;
import io.harness.distribution.constraint.UnableToRegisterConsumerException;
import io.harness.distribution.constraint.UnableToSaveConstraintException;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;
import io.harness.validation.Create;
import io.harness.validation.Update;
import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraint.ResourceConstraintKeys;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceBuilder;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;
import software.wings.beans.ResourceConstraintUsage;
import software.wings.beans.ResourceConstraintUsage.ActiveScope;
import software.wings.beans.ResourceConstraintUsage.ActiveScope.ActiveScopeBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ResourceConstraintStatusData;
import software.wings.sm.states.ResourceConstraintState.HoldingScope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class ResourceConstraintServiceImpl implements ResourceConstraintService, ConstraintRegistry {
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<ResourceConstraint> list(PageRequest<ResourceConstraint> pageRequest) {
    return wingsPersistence.query(ResourceConstraint.class, pageRequest);
  }

  @Override
  @ValidationGroups(Create.class)
  public ResourceConstraint save(ResourceConstraint resourceConstraint) {
    try {
      wingsPersistence.save(resourceConstraint);
      return resourceConstraint;
    } catch (DuplicateKeyException exception) {
      throw new InvalidRequestException("The resource constraint name cannot be reused.", exception, USER);
    }
  }

  @Override
  @ValidationGroups(Update.class)
  public void update(ResourceConstraint resourceConstraint) {
    wingsPersistence.merge(resourceConstraint);
  }

  @Override
  public ResourceConstraint get(String accountId, String resourceConstraintId) {
    final ResourceConstraint resourceConstraint = wingsPersistence.get(ResourceConstraint.class, resourceConstraintId);
    if (resourceConstraint != null && accountId != null && !resourceConstraint.getAccountId().equals(accountId)) {
      return null;
    }
    return resourceConstraint;
  }

  private void ensureSafeToDelete(String accountId, String resourceConstraintId) {
    // TODO: add the needed logic to check if resourceConstraintId is currently in use.
  }

  @Override
  public void delete(String accountId, String resourceConstraintId) {
    final ResourceConstraint resourceConstraint = wingsPersistence.get(ResourceConstraint.class, resourceConstraintId);
    if (resourceConstraint == null || !resourceConstraint.getAccountId().equals(accountId)) {
      logger.error("Some attempted to delete resource constraint that belongs to different account");
      return;
    }

    ensureSafeToDelete(accountId, resourceConstraintId);
    wingsPersistence.delete(ResourceConstraint.class, resourceConstraintId);
  }

  @Override
  public Constraint createAbstraction(ResourceConstraint resourceConstraint) {
    return Constraint.builder()
        .id(new ConstraintId(resourceConstraint.getUuid()))
        .spec(Constraint.Spec.builder()
                  .limits(resourceConstraint.getCapacity())
                  .strategy(resourceConstraint.getStrategy())
                  .build())
        .build();
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(ResourceConstraint.class).filter(ResourceConstraint.ACCOUNT_ID_KEY, accountId));
  }

  @Override
  public Set<String> updateActiveConstraints(String appId, String workflowExecutionId) {
    final Query<ResourceConstraintInstance> query =
        wingsPersistence.createQuery(ResourceConstraintInstance.class, appId == null ? excludeAuthority : allChecks)
            .filter(ResourceConstraintInstanceKeys.state, State.ACTIVE.name());

    if (appId != null) {
      query.filter(ResourceConstraintInstanceKeys.appId, appId);
    }
    if (workflowExecutionId != null) {
      query.filter(ResourceConstraintInstanceKeys.releaseEntityType, WORKFLOW.name())
          .filter(ResourceConstraintInstanceKeys.releaseEntityId, workflowExecutionId);
    }

    Set<String> constraintIds = new HashSet<>();
    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(query.fetch())) {
      for (ResourceConstraintInstance instance : iterator) {
        final HoldingScope holdingScope = HoldingScope.valueOf(instance.getReleaseEntityType());
        switch (holdingScope) {
          case WORKFLOW:
            final WorkflowExecution workflowExecution =
                workflowExecutionService.getWorkflowExecution(instance.getAppId(), instance.getReleaseEntityId());
            if (workflowExecution == null || ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
              Map<String, Object> constraintContext =
                  ImmutableMap.of(ResourceConstraintInstanceKeys.appId, instance.getAppId());

              if (getRegistry().consumerFinished(new ConstraintId(instance.getResourceConstraintId()),
                      new ConstraintUnit(instance.getResourceUnit()), new ConsumerId(instance.getUuid()),
                      constraintContext)) {
                constraintIds.add(instance.getResourceConstraintId());
              }
            }
            break;

          default:
            unhandled(holdingScope);
        }
      }
    }

    return constraintIds;
  }

  @Override
  public Set<String> selectBlockedConstraints() {
    Set<String> constraintIds = new HashSet<>();
    Set<String> excludeConstraintIds = new HashSet<>();
    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(
             wingsPersistence.createQuery(ResourceConstraintInstance.class, excludeAuthority)
                 .field(ResourceConstraintInstanceKeys.state)
                 .notEqual(State.FINISHED.name())
                 .fetch())) {
      for (ResourceConstraintInstance instance : iterator) {
        if (State.ACTIVE.name().equals(instance.getState())) {
          excludeConstraintIds.add(instance.getResourceConstraintId());
          constraintIds.remove(instance.getResourceConstraintId());
          continue;
        }
        if (State.BLOCKED.name().equals(instance.getState())) {
          if (!excludeConstraintIds.contains(instance.getResourceConstraintId())) {
            constraintIds.add(instance.getResourceConstraintId());
          }
          continue;
        }
        unhandled(instance.getState());
      }
    }
    return constraintIds;
  }

  private Query<ResourceConstraintInstance> runnableQuery(String constraintId) {
    return wingsPersistence.createQuery(ResourceConstraintInstance.class, excludeAuthority)
        .filter(ResourceConstraintInstanceKeys.resourceConstraintId, constraintId)
        .field(ResourceConstraintInstanceKeys.state)
        .in(asList(State.BLOCKED.name(), State.ACTIVE.name()));
  }

  private List<ConstraintUnit> units(ResourceConstraint constraint) {
    Set<String> units = new HashSet<>();
    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(
             runnableQuery(constraint.getUuid()).project(ResourceConstraintInstanceKeys.resourceUnit, true).fetch())) {
      for (ResourceConstraintInstance instance : iterator) {
        units.add(instance.getResourceUnit());
      }
    }
    return units.stream().map(unit -> new ConstraintUnit(unit)).collect(toList());
  }

  @Override
  public void updateBlockedConstraints(Set<String> constraintIds) {
    if (isEmpty(constraintIds)) {
      return;
    }

    try (HIterator<ResourceConstraint> iterator =
             new HIterator<ResourceConstraint>(wingsPersistence.createQuery(ResourceConstraint.class, excludeAuthority)
                                                   .field(ResourceConstraintKeys.uuid)
                                                   .in(constraintIds)
                                                   .fetch())) {
      for (ResourceConstraint instance : iterator) {
        final Constraint constraint = createAbstraction(instance);
        final List<ConstraintUnit> units = units(instance);
        if (isEmpty(units)) {
          continue;
        }

        logger.info("Resource constraint {} has running units {}", instance.getUuid(), Joiner.on(", ").join(units));

        units.forEach(unit -> {
          final RunnableConsumers runnableConsumers = constraint.runnableConsumers(unit, getRegistry());
          for (ConsumerId consumerId : runnableConsumers.getConsumerIds()) {
            if (!constraint.consumerUnblocked(unit, consumerId, null, getRegistry())) {
              break;
            }
          }
        });
      }
    }
  }

  @Override
  public List<ResourceConstraintUsage> usage(String accountId, List<String> resourceConstraintIds) {
    Map<String, List<ResourceConstraintUsage.ActiveScope>> map = new HashMap<>();

    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(
             wingsPersistence.createQuery(ResourceConstraintInstance.class, excludeAuthority)
                 .field(ResourceConstraintInstanceKeys.resourceConstraintId)
                 .in(resourceConstraintIds)
                 .filter(ResourceConstraintInstanceKeys.state, State.ACTIVE.name())
                 .order(Sort.ascending(ResourceConstraintInstanceKeys.order))
                 .fetch())) {
      for (ResourceConstraintInstance instance : iterator) {
        final List<ActiveScope> activeScopes =
            map.computeIfAbsent(instance.getResourceConstraintId(), key -> new ArrayList<ActiveScope>());

        final ActiveScopeBuilder builder = ActiveScope.builder()
                                               .releaseEntityType(instance.getReleaseEntityType())
                                               .releaseEntityId(instance.getReleaseEntityId())
                                               .unit(instance.getResourceUnit())
                                               .permits(instance.getPermits())
                                               .acquiredAt(instance.getAcquiredAt());

        HoldingScope scope = HoldingScope.valueOf(instance.getReleaseEntityType());
        switch (scope) {
          case WORKFLOW:
            final WorkflowExecution workflowExecution =
                workflowExecutionService.getWorkflowExecution(instance.getAppId(), instance.getReleaseEntityId());
            builder.releaseEntityName(workflowExecution.normalizedName());
            break;
          default:
            unhandled(scope);
        }

        activeScopes.add(builder.build());
      }
    }

    return map.entrySet()
        .stream()
        .map(entry
            -> ResourceConstraintUsage.builder()
                   .resourceConstraintId(entry.getKey())
                   .activeScopes(entry.getValue())
                   .build())
        .collect(toList());
  }

  @Override
  public ConstraintRegistry getRegistry() {
    return (ConstraintRegistry) this;
  }

  @Override
  public int getMaxOrder(String resourceConstraintId) {
    final ResourceConstraintInstance resourceConstraintInstance =
        wingsPersistence.createQuery(ResourceConstraintInstance.class)
            .filter(ResourceConstraintInstanceKeys.resourceConstraintId, resourceConstraintId)
            .order(Sort.descending(ResourceConstraintInstanceKeys.order))
            .get(new FindOptions().limit(1));

    if (resourceConstraintInstance == null) {
      return 0;
    }

    return resourceConstraintInstance.getOrder();
  }

  @Override
  public void save(ConstraintId id, Spec spec) throws UnableToSaveConstraintException {}

  @Override
  public Constraint load(ConstraintId id) throws UnableToLoadConstraintException {
    final ResourceConstraint resourceConstraint = get(null, id.getValue());
    return createAbstraction(resourceConstraint);
  }

  @Override
  public List<Consumer> loadConsumers(ConstraintId id, ConstraintUnit unit) {
    List<Consumer> consumers = new ArrayList<>();

    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(
             runnableQuery(id.getValue())
                 .filter(ResourceConstraintInstanceKeys.resourceUnit, unit.getValue())
                 .order(Sort.ascending(ResourceConstraintInstanceKeys.order))
                 .fetch())) {
      for (ResourceConstraintInstance instance : iterator) {
        consumers.add(Consumer.builder()
                          .id(new ConsumerId(instance.getUuid()))
                          .state(State.valueOf(instance.getState()))
                          .permits(instance.getPermits())
                          .context(ImmutableMap.of(ResourceConstraintInstanceKeys.releaseEntityType,
                              instance.getReleaseEntityType(), ResourceConstraintInstanceKeys.releaseEntityId,
                              instance.getReleaseEntityId()))
                          .build());
      }
    }

    return consumers;
  }

  @Override
  public boolean overlappingScope(Consumer consumer, Consumer blockedConsumer) {
    String releaseScope = (String) consumer.getContext().get(ResourceConstraintInstanceKeys.releaseEntityType);
    String blockedReleaseScope =
        (String) blockedConsumer.getContext().get(ResourceConstraintInstanceKeys.releaseEntityType);

    if (!WORKFLOW.name().equals(releaseScope)) {
      unhandled(releaseScope);
      return false;
    }
    if (!WORKFLOW.name().equals(blockedReleaseScope)) {
      unhandled(blockedReleaseScope);
      return false;
    }

    String workflowExecutionId = (String) consumer.getContext().get(ResourceConstraintInstanceKeys.releaseEntityId);
    String blockedWorkflowExecutionId =
        (String) blockedConsumer.getContext().get(ResourceConstraintInstanceKeys.releaseEntityId);

    return workflowExecutionId.equals(blockedWorkflowExecutionId);
  }

  @Override
  public boolean registerConsumer(ConstraintId id, ConstraintUnit unit, Consumer consumer, int currentlyRunning)
      throws UnableToRegisterConsumerException {
    ResourceConstraint resourceConstraint = get(null, id.getValue());
    if (resourceConstraint == null) {
      throw new InvalidRequestException(format("There is no resource constraint with id: %s", id.getValue()));
    }

    final ResourceConstraintInstanceBuilder builder =
        ResourceConstraintInstance.builder()
            .uuid(consumer.getId().getValue())
            .appId((String) consumer.getContext().get(ResourceConstraintInstanceKeys.appId))
            .resourceConstraintId(id.getValue())
            .resourceUnit(unit.getValue())
            .releaseEntityType((String) consumer.getContext().get(ResourceConstraintInstanceKeys.releaseEntityType))
            .releaseEntityId((String) consumer.getContext().get(ResourceConstraintInstanceKeys.releaseEntityId))
            .permits(consumer.getPermits())
            .state(consumer.getState().name())
            .order((int) consumer.getContext().get(ResourceConstraintInstanceKeys.order));

    if (consumer.getState() == State.ACTIVE) {
      builder.acquiredAt(currentTimeMillis());
    }

    try {
      wingsPersistence.save(builder.build());
    } catch (DuplicateKeyException exception) {
      logger.info("Failed to add ResourceConstraintInstance", exception);
      return false;
    }
    return true;
  }

  @Override
  public boolean adjustRegisterConsumerContext(ConstraintId id, Map<String, Object> context) {
    final int order = getMaxOrder(id.getValue()) + 1;
    if (order == (int) context.get(ResourceConstraintInstanceKeys.order)) {
      return false;
    }
    context.put(ResourceConstraintInstanceKeys.order, order);
    return true;
  }

  @Override
  public boolean consumerUnblocked(
      ConstraintId id, ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context) {
    waitNotifyEngine.notify(consumerId.getValue(), ResourceConstraintStatusData.builder().build());

    final Query<ResourceConstraintInstance> query =
        wingsPersistence.createQuery(ResourceConstraintInstance.class)
            .filter(ResourceConstraintInstanceKeys.uuid, consumerId.getValue())
            .filter(ResourceConstraintInstanceKeys.resourceUnit, unit.getValue())
            .filter(ResourceConstraintInstanceKeys.state, State.BLOCKED.name());

    if (context != null && context.containsKey(ResourceConstraintInstanceKeys.appId)) {
      query.filter(ResourceConstraintInstanceKeys.appId, context.get(ResourceConstraintInstanceKeys.appId));
    }

    final UpdateOperations<ResourceConstraintInstance> ops =
        wingsPersistence.createUpdateOperations(ResourceConstraintInstance.class)
            .set(ResourceConstraintInstanceKeys.state, State.ACTIVE.name())
            .set(ResourceConstraintInstanceKeys.acquiredAt, currentTimeMillis());

    wingsPersistence.update(query, ops);
    return true;
  }

  @Override
  public boolean consumerFinished(
      ConstraintId id, ConstraintUnit unit, ConsumerId consumerId, Map<String, Object> context) {
    final Query<ResourceConstraintInstance> query =
        wingsPersistence.createQuery(ResourceConstraintInstance.class)
            .filter(ResourceConstraintInstanceKeys.appId, context.get(ResourceConstraintInstanceKeys.appId))
            .filter(ResourceConstraintInstanceKeys.uuid, consumerId.getValue())
            .filter(ResourceConstraintInstanceKeys.resourceUnit, unit.getValue())
            .filter(ResourceConstraintInstanceKeys.state, State.ACTIVE.name());

    final UpdateOperations<ResourceConstraintInstance> ops =
        wingsPersistence.createUpdateOperations(ResourceConstraintInstance.class)
            .set(ResourceConstraintInstanceKeys.state, State.FINISHED.name());

    final UpdateResults updateResults = wingsPersistence.update(query, ops);
    if (updateResults == null || updateResults.getUpdatedCount() == 0) {
      logger.error("The attempt to finish {}.{} for {} failed", id.getValue(), unit.getValue(), consumerId.getValue());
      return false;
    }
    return true;
  }
}
