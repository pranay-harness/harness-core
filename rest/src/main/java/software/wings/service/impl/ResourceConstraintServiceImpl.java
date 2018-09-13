package software.wings.service.impl;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.sm.states.ResourceConstraintState.HoldingScope.WORKFLOW;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.distribution.constraint.Constraint;
import io.harness.distribution.constraint.Constraint.Spec;
import io.harness.distribution.constraint.ConstraintId;
import io.harness.distribution.constraint.ConstraintRegistry;
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
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceBuilder;
import software.wings.beans.ResourceConstraintUsage;
import software.wings.beans.ResourceConstraintUsage.ActiveScope;
import software.wings.beans.ResourceConstraintUsage.ActiveScope.ActiveScopeBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.ResourceConstraintStatusData;
import software.wings.sm.states.ResourceConstraintState.HoldingScope;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class ResourceConstraintServiceImpl implements ResourceConstraintService, ConstraintRegistry {
  private static final Logger logger = LoggerFactory.getLogger(ResourceConstraintServiceImpl.class);

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
    return wingsPersistence.saveAndGet(ResourceConstraint.class, resourceConstraint);
  }

  @Override
  @ValidationGroups(Update.class)
  public ResourceConstraint update(ResourceConstraint resourceConstraint) {
    return wingsPersistence.saveAndGet(ResourceConstraint.class, resourceConstraint);
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
    wingsPersistence.delete(wingsPersistence.createQuery(ResourceConstraint.class).filter(ACCOUNT_ID_KEY, accountId));
  }

  @Override
  public Set<String> updateActiveConstraints(String appId, String workflowExecutionId) {
    final Query<ResourceConstraintInstance> query =
        wingsPersistence.createQuery(ResourceConstraintInstance.class)
            .filter(ResourceConstraintInstance.STATE_KEY, State.ACTIVE.name());

    if (appId != null) {
      query.filter(ResourceConstraintInstance.APP_ID_KEY, appId);
    }
    if (workflowExecutionId != null) {
      query.filter(ResourceConstraintInstance.RELEASE_ENTITY_TYPE_KEY, WORKFLOW.name())
          .filter(ResourceConstraintInstance.RELEASE_ENTITY_ID_KEY, workflowExecutionId);
    }

    Set<String> constraintIds = new HashSet<>();
    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(query.fetch())) {
      while (iterator.hasNext()) {
        ResourceConstraintInstance instance = iterator.next();
        final HoldingScope holdingScope = HoldingScope.valueOf(instance.getReleaseEntityType());
        switch (holdingScope) {
          case WORKFLOW:
            final WorkflowExecution workflowExecution =
                workflowExecutionService.getWorkflowExecution(instance.getAppId(), instance.getReleaseEntityId());
            if (ExecutionStatus.isFinalStatus(workflowExecution.getStatus())) {
              Map<String, Object> constraintContext =
                  ImmutableMap.of(ResourceConstraintInstance.APP_ID_KEY, instance.getAppId());

              if (getRegistry().consumerFinished(new ConstraintId(instance.getResourceConstraintId()),
                      new ConsumerId(instance.getUuid()), constraintContext)) {
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
    try (HIterator<ResourceConstraintInstance> iterator =
             new HIterator<ResourceConstraintInstance>(wingsPersistence.createQuery(ResourceConstraintInstance.class)
                                                           .field(ResourceConstraintInstance.STATE_KEY)
                                                           .notEqual(State.FINISHED.name())
                                                           .fetch())) {
      while (iterator.hasNext()) {
        ResourceConstraintInstance instance = iterator.next();
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

  @Override
  public void updateBlockedConstraints(Set<String> constraintIds) {
    try (HIterator<ResourceConstraint> iterator =
             new HIterator<ResourceConstraint>(wingsPersistence.createQuery(ResourceConstraint.class)
                                                   .field(ResourceConstraint.ID_KEY)
                                                   .in(constraintIds)
                                                   .fetch())) {
      while (iterator.hasNext()) {
        ResourceConstraint instance = iterator.next();
        final Constraint constraint = createAbstraction(instance);

        final RunnableConsumers runnableConsumers = constraint.runnableConsumers(getRegistry());
        for (ConsumerId consumerId : runnableConsumers.getConsumerIds()) {
          if (!constraint.consumerUnblocked(consumerId, null, getRegistry())) {
            break;
          }
        }
      }
    }
  }

  @Override
  public List<ResourceConstraintUsage> usage(String accountId, List<String> resourceConstraintIds) {
    Map<String, List<ResourceConstraintUsage.ActiveScope>> map = new HashMap<>();

    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(
             wingsPersistence.createQuery(ResourceConstraintInstance.class)
                 .field(ResourceConstraintInstance.RESOURCE_CONSTRAINT_ID_KEY)
                 .in(resourceConstraintIds)
                 .filter(ResourceConstraintInstance.STATE_KEY, State.ACTIVE.name())
                 .order(Sort.ascending(ResourceConstraintInstance.ORDER_KEY))
                 .fetch())) {
      while (iterator.hasNext()) {
        ResourceConstraintInstance instance = iterator.next();
        final List<ActiveScope> activeScopes =
            map.computeIfAbsent(instance.getResourceConstraintId(), key -> new ArrayList<ActiveScope>());

        final ActiveScopeBuilder builder = ActiveScope.builder()
                                               .releaseEntityType(instance.getReleaseEntityType())
                                               .releaseEntityId(instance.getReleaseEntityId())
                                               .permits(instance.getPermits())
                                               .acquiredAt(instance.getAcquiredAt());

        HoldingScope scope = HoldingScope.valueOf(instance.getReleaseEntityType());
        switch (scope) {
          case WORKFLOW:
            final WorkflowExecution workflowExecution =
                workflowExecutionService.getWorkflowExecution(instance.getAppId(), instance.getReleaseEntityId());
            builder.releaseEntityName(workflowExecution.getName());
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
        .collect(Collectors.toList());
  }

  @Override
  public ConstraintRegistry getRegistry() {
    return (ConstraintRegistry) this;
  }

  @Override
  public int getMaxOrder(String resourceConstraintId) {
    final ResourceConstraintInstance resourceConstraintInstance =
        wingsPersistence.createQuery(ResourceConstraintInstance.class)
            .filter(ResourceConstraintInstance.RESOURCE_CONSTRAINT_ID_KEY, resourceConstraintId)
            .order(Sort.descending(ResourceConstraintInstance.ORDER_KEY))
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
  public List<Consumer> loadConsumers(ConstraintId id) {
    List<Consumer> consumers = new ArrayList<>();

    try (HIterator<ResourceConstraintInstance> iterator = new HIterator<ResourceConstraintInstance>(
             wingsPersistence.createQuery(ResourceConstraintInstance.class)
                 .filter(ResourceConstraintInstance.RESOURCE_CONSTRAINT_ID_KEY, id.getValue())
                 .field(ResourceConstraintInstance.STATE_KEY)
                 .in(asList(State.BLOCKED.name(), State.ACTIVE.name()))
                 .order(Sort.ascending(ResourceConstraintInstance.ORDER_KEY))
                 .fetch())) {
      while (iterator.hasNext()) {
        ResourceConstraintInstance instance = iterator.next();
        consumers.add(Consumer.builder()
                          .id(new ConsumerId(instance.getUuid()))
                          .state(State.valueOf(instance.getState()))
                          .permits(instance.getPermits())
                          .context(ImmutableMap.of(ResourceConstraintInstance.RELEASE_ENTITY_TYPE_KEY,
                              instance.getReleaseEntityType(), ResourceConstraintInstance.RELEASE_ENTITY_ID_KEY,
                              instance.getReleaseEntityId()))
                          .build());
      }
    }

    return consumers;
  }

  @Override
  public boolean overlappingScope(Consumer consumer, Consumer blockedConsumer) {
    String releaseScope = (String) consumer.getContext().get(ResourceConstraintInstance.RELEASE_ENTITY_TYPE_KEY);
    String blockedReleaseScope =
        (String) blockedConsumer.getContext().get(ResourceConstraintInstance.RELEASE_ENTITY_TYPE_KEY);

    if (!WORKFLOW.name().equals(releaseScope)) {
      unhandled(releaseScope);
      return false;
    }
    if (!WORKFLOW.name().equals(blockedReleaseScope)) {
      unhandled(blockedReleaseScope);
      return false;
    }

    String workflowExecutionId = (String) consumer.getContext().get(ResourceConstraintInstance.RELEASE_ENTITY_ID_KEY);
    String blockedWorkflowExecutionId =
        (String) blockedConsumer.getContext().get(ResourceConstraintInstance.RELEASE_ENTITY_ID_KEY);

    return workflowExecutionId.equals(blockedWorkflowExecutionId);
  }

  @Override
  public boolean registerConsumer(ConstraintId id, Consumer consumer, int currentlyRunning)
      throws UnableToRegisterConsumerException {
    ResourceConstraint resourceConstraint = get(null, id.getValue());
    if (resourceConstraint == null) {
      throw new InvalidRequestException(format("There is no resource constraint with id: %s", id.getValue()));
    }

    final ResourceConstraintInstanceBuilder builder =
        ResourceConstraintInstance.builder()
            .uuid(consumer.getId().getValue())
            .appId((String) consumer.getContext().get(ResourceConstraintInstance.APP_ID_KEY))
            .resourceConstraintId(id.getValue())
            .releaseEntityType((String) consumer.getContext().get(ResourceConstraintInstance.RELEASE_ENTITY_TYPE_KEY))
            .releaseEntityId((String) consumer.getContext().get(ResourceConstraintInstance.RELEASE_ENTITY_ID_KEY))
            .permits(consumer.getPermits())
            .state(consumer.getState().name())
            .order((int) consumer.getContext().get(ResourceConstraintInstance.ORDER_KEY));

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
    if (order == (int) context.get(ResourceConstraintInstance.ORDER_KEY)) {
      return false;
    }
    context.put(ResourceConstraintInstance.ORDER_KEY, order);
    return true;
  }

  @Override
  public boolean consumerUnblocked(ConstraintId id, ConsumerId consumerId, Map<String, Object> context) {
    waitNotifyEngine.notify(consumerId.getValue(), ResourceConstraintStatusData.builder().build());

    final Query<ResourceConstraintInstance> query =
        wingsPersistence.createQuery(ResourceConstraintInstance.class)
            .filter(ResourceConstraintInstance.ID_KEY, consumerId.getValue())
            .filter(ResourceConstraintInstance.STATE_KEY, State.BLOCKED.name());

    if (context != null && context.containsKey(ResourceConstraintInstance.APP_ID_KEY)) {
      query.filter(ResourceConstraintInstance.APP_ID_KEY, context.get(ResourceConstraintInstance.APP_ID_KEY));
    }

    final UpdateOperations<ResourceConstraintInstance> ops =
        wingsPersistence.createUpdateOperations(ResourceConstraintInstance.class)
            .set(ResourceConstraintInstance.STATE_KEY, State.ACTIVE.name())
            .set(ResourceConstraintInstance.ACQUIRED_AT_KEY, currentTimeMillis());

    wingsPersistence.update(query, ops);
    return true;
  }

  @Override
  public boolean consumerFinished(ConstraintId id, ConsumerId consumerId, Map<String, Object> context) {
    final Query<ResourceConstraintInstance> query =
        wingsPersistence.createQuery(ResourceConstraintInstance.class)
            .filter(ResourceConstraintInstance.APP_ID_KEY, context.get(ResourceConstraintInstance.APP_ID_KEY))
            .filter(ResourceConstraintInstance.ID_KEY, consumerId.getValue())
            .filter(ResourceConstraintInstance.STATE_KEY, State.ACTIVE.name());

    final UpdateOperations<ResourceConstraintInstance> ops =
        wingsPersistence.createUpdateOperations(ResourceConstraintInstance.class)
            .set(ResourceConstraintInstance.STATE_KEY, State.FINISHED.name());

    wingsPersistence.update(query, ops);
    return true;
  }
}
