/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml;

import static io.harness.govern.Switch.unhandled;
import static io.harness.validation.Validator.notNullCheck;
import static io.harness.validation.Validator.nullCheck;

import io.harness.ff.FeatureFlagService;
import io.harness.git.model.ChangeType;
import io.harness.observer.Subject;
import io.harness.persistence.UuidAware;

import software.wings.beans.Application;
import software.wings.beans.Event.Type;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.entitycrud.EntityCrudOperationObserver;
import software.wings.service.intfc.yaml.YamlPushService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.validation.executable.ValidateOnExecution;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ValidateOnExecution
@Singleton
@Slf4j
public class YamlPushServiceImpl implements YamlPushService {
  private static final String YAML_PUSH_SERVICE_LOG = "YAML_PUSH_SERVICE_LOG";

  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject private ExecutorService executorService;
  @Inject private YamlHelper yamlHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Getter private Subject<EntityCrudOperationObserver> entityCrudSubject = new Subject<>();

  @Override
  public <T> Future<?> pushYamlChangeSet(
      String accountId, T oldEntity, T newEntity, Type type, boolean syncFromGit, boolean isRename) {
    return executorService.submit(() -> {
      try {
        performAuditForEntityChange(accountId, oldEntity, newEntity, type);

        if (syncFromGit) {
          return;
        }

        switch (type) {
          case CREATE:
            validateCreate(oldEntity, newEntity);
            logYamlPushRequestInfo(oldEntity, newEntity, type);
            pushYamlChangeSetOnCreate(accountId, newEntity);
            break;

          case UPDATE:
            validateUpdate(oldEntity, newEntity);
            logYamlPushRequestInfo(oldEntity, newEntity, type);
            pushYamlChangeSetOnUpdate(accountId, oldEntity, newEntity, isRename);
            break;

          case DELETE:
            validateDelete(oldEntity, newEntity);
            logYamlPushRequestInfo(oldEntity, newEntity, type);
            pushYamlChangeSetOnDelete(accountId, oldEntity);
            break;

          default:
            unhandled(type);
        }
      } catch (Exception e) {
        logYamlPushRequestInfo(oldEntity, newEntity, type);
        log.error("Exception in pushing yaml change set", e);
      }
    });
  }

  private <T> void performAuditForEntityChange(String accountId, T oldEntity, T newEntity, Type type) {
    try {
      entityCrudSubject.fireInform(
          EntityCrudOperationObxxxxxxxx:handleEntityCrudOperation, accountId, oldEntity, newEntity, type);
    } catch (Exception e) {
      logErrorMsgForAudit(oldEntity, newEntity, type, e);
    }
  }

  private <T> void logErrorMsgForAudit(T oldEntity, T newEntity, Type type, Exception e) {
    log.error(new StringBuilder(128)
                  .append("Failed while trying to perform audit for Entity: ")
                  .append(oldEntity != null ? oldEntity.getClass() : newEntity.getClass())
                  .append(", ID: ")
                  .append(oldEntity != null ? ((UuidAware) oldEntity).getUuid() : ((UuidAware) newEntity).getUuid())
                  .append("Operation Type: ")
                  .append(type.name())
                  .append(", Exception: ")
                  .append(e)
                  .toString());
  }

  @Override
  public <R, T> void pushYamlChangeSet(String accountId, R helperEntity, T entity, Type type, boolean syncFromGit) {
    performAuditForEntityChange(accountId, entity, entity, type);

    if (syncFromGit) {
      return;
    }

    executorService.submit(() -> {
      try {
        notNullCheck("entity", entity);
        notNullCheck("helperEntity", helperEntity);
        notNullCheck("accountId", accountId);

        logYamlPushRequestInfo(accountId, helperEntity, entity);

        switch (type) {
          case CREATE:
            yamlChangeSetHelper.entityYamlChangeSet(accountId, helperEntity, entity, ChangeType.ADD);
            break;

          case UPDATE:
            yamlChangeSetHelper.entityYamlChangeSet(accountId, helperEntity, entity, ChangeType.MODIFY);
            break;

          case DELETE:
            if (canGenerateYamlPath(entity)) {
              yamlChangeSetHelper.entityYamlChangeSet(accountId, helperEntity, entity, ChangeType.DELETE);
            }
            break;

          default:
            unhandled(type);
        }
      } catch (Exception e) {
        logYamlPushRequestInfo(accountId, helperEntity, entity);
        log.error("Exception in pushing yaml change set for account {}", accountId, e);
      }
    });
  }

  private <T> void validateCreate(T oldEntity, T newEntity) {
    nullCheck("oldEntity", oldEntity);
    notNullCheck("newEntity", newEntity);
  }

  private <T> void validateUpdate(T oldEntity, T newEntity) {
    notNullCheck("oldEntity", oldEntity);
    notNullCheck("newEntity", newEntity);
  }

  private <T> void validateDelete(T oldEntity, T newEntity) {
    notNullCheck("oldEntity", oldEntity);
    nullCheck("newEntity", newEntity);
  }

  private <T> void pushYamlChangeSetOnCreate(String accountId, T entity) {
    yamlChangeSetHelper.entityYamlChangeSet(accountId, entity, ChangeType.ADD);
  }

  private <T> void pushYamlChangeSetOnUpdate(String accountId, T oldEntity, T newEntity, boolean isRename) {
    yamlChangeSetHelper.entityUpdateYamlChange(accountId, oldEntity, newEntity, isRename);
  }

  private <T> void pushYamlChangeSetOnDelete(String accountId, T entity) {
    if (canGenerateYamlPath(entity)) {
      yamlChangeSetHelper.entityYamlChangeSet(accountId, entity, ChangeType.DELETE);
    }
  }

  /**
   * When any app, service is deleted, all nested entities are deleted in background thread.
   * If we fail to generate yaml path, then we cannot push that change to git
   *
   * @param entity
   * @return
   */
  private boolean canGenerateYamlPath(Object entity) {
    if (entity instanceof Application) {
      return true;
    }

    try {
      yamlHelper.getYamlPathForEntity(entity);
    } catch (Exception e) {
      // If we fail to generate yaml path, then we cannot push that change to git.
      log.info("Exception while generating yaml path", e);
      return false;
    }

    return true;
  }

  @Override
  public void pushYamlChangeSet(String accountId, String appId, ChangeType changeType, boolean syncFromGit) {
    if (syncFromGit) {
      return;
    }

    executorService.submit(() -> {
      try {
        yamlChangeSetHelper.defaultVariableChangeSet(accountId, appId, changeType);
      } catch (Exception e) {
        log.error("Exception in pushing yaml change set for account {}", accountId, e);
      }
    });
  }

  private <R, T> void logYamlPushRequestInfo(String accountId, R helperEntity, T entity) {
    log.info("{} accountId {}, entity {}, entityId {}, helperEntityId {}", YAML_PUSH_SERVICE_LOG, accountId,
        entity.getClass().getSimpleName(), ((UuidAware) entity).getUuid(), ((UuidAware) helperEntity).getUuid());
  }

  private <T> void logYamlPushRequestInfo(T oldEntity, T newEntity, Type type) {
    StringBuilder builder = new StringBuilder(50);
    builder.append(YAML_PUSH_SERVICE_LOG);

    switch (type) {
      case CREATE:
        builder.append(", ")
            .append(type.name())
            .append(", entity ")
            .append(newEntity.getClass().getSimpleName())
            .append(", entityId ")
            .append(((UuidAware) newEntity).getUuid());
        break;

      case UPDATE:
        builder.append(", ")
            .append(type.name())
            .append(", entity ")
            .append(oldEntity.getClass().getSimpleName())
            .append(", oldEntityId ")
            .append(((UuidAware) oldEntity).getUuid())
            .append(", newEntityId ")
            .append(((UuidAware) newEntity).getUuid());
        break;

      case DELETE:
        builder.append(", ")
            .append(type.name())
            .append(", entity ")
            .append(oldEntity.getClass().getSimpleName())
            .append(", entityId ")
            .append(((UuidAware) oldEntity).getUuid());

        break;

      default:
        unhandled(type);
    }

    log.info(builder.toString());
  }
}
