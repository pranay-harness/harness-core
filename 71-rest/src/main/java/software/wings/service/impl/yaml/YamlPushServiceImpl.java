package software.wings.service.impl.yaml;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.UuidAware;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Event.Type;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.utils.Validator;

import java.util.concurrent.ExecutorService;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class YamlPushServiceImpl implements YamlPushService {
  private static final String YAML_PUSH_SERVICE_LOG = "YAML_PUSH_SERVICE_LOG";

  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject private ExecutorService executorService;
  @Inject private YamlHelper yamlHelper;
  @Inject private AuditService auditService;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public <T> void pushYamlChangeSet(
      String accountId, T oldEntity, T newEntity, Type type, boolean syncFromGit, boolean isRename) {
    executorService.submit(() -> {
      try {
        performAuditForEntityChange(accountId, oldEntity, newEntity, type);

        if (syncFromGit) {
          return;
        }

        switch (type) {
          case CREATE:
            validateCreate(oldEntity, newEntity);
            logYamlPushRequestInfo(accountId, oldEntity, newEntity, type);
            pushYamlChangeSetOnCreate(accountId, newEntity);
            break;

          case UPDATE:
            validateUpdate(oldEntity, newEntity);
            logYamlPushRequestInfo(accountId, oldEntity, newEntity, type);
            pushYamlChangeSetOnUpdate(accountId, oldEntity, newEntity, isRename);
            break;

          case DELETE:
            validateDelete(oldEntity, newEntity);
            logYamlPushRequestInfo(accountId, oldEntity, newEntity, type);
            pushYamlChangeSetOnDelete(accountId, oldEntity);
            break;

          default:
            unhandled(type);
        }
      } catch (Exception e) {
        logYamlPushRequestInfo(accountId, oldEntity, newEntity, type);
        logger.error(format("Exception in pushing yaml change set for account %s", accountId), e);
      }
    });
  }

  private <T> void performAuditForEntityChange(String accountId, T oldEntity, T newEntity, Type type) {
    try {
      auditService.registerAuditActions(accountId, oldEntity, newEntity, type);
    } catch (Exception e) {
      getErrorMsgForAudit(oldEntity, newEntity, type, e);
    }
  }

  private <T> void getErrorMsgForAudit(T oldEntity, T newEntity, Type type, Exception e) {
    logger.error(new StringBuilder(128)
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
            yamlChangeSetHelper.entityYamlChangeSet(accountId, helperEntity, entity, ChangeType.DELETE);
            break;

          default:
            unhandled(type);
        }
      } catch (Exception e) {
        logYamlPushRequestInfo(accountId, helperEntity, entity);
        logger.error(format("Exception in pushing yaml change set for account %s", accountId), e);
      }
    });
  }

  private <T> void validateCreate(T oldEntity, T newEntity) {
    Validator.nullCheck("oldEntity", oldEntity);
    notNullCheck("newEntity", newEntity);
  }

  private <T> void validateUpdate(T oldEntity, T newEntity) {
    notNullCheck("oldEntity", oldEntity);
    notNullCheck("newEntity", newEntity);
  }

  private <T> void validateDelete(T oldEntity, T newEntity) {
    notNullCheck("oldEntity", oldEntity);
    Validator.nullCheck("newEntity", newEntity);
  }

  private <T> void pushYamlChangeSetOnCreate(String accountId, T entity) {
    yamlChangeSetHelper.entityYamlChangeSet(accountId, entity, ChangeType.ADD);
  }

  private <T> void pushYamlChangeSetOnUpdate(String accountId, T oldEntity, T newEntity, boolean isRename) {
    yamlChangeSetHelper.entityUpdateYamlChange(accountId, oldEntity, newEntity, isRename);
  }

  private <T> void pushYamlChangeSetOnDelete(String accountId, T entity) {
    if (parentEntityExists(accountId, entity)) {
      yamlChangeSetHelper.entityYamlChangeSet(accountId, entity, ChangeType.DELETE);
    }
  }

  /**
   * When any app, service is deleted, all nested entities are deleted in background thread.
   * So lets say, service was deleted. This change will propogate to git and remove entire service folder.
   *
   * Now, in background thread, when artifact stream, ApplicationManifestFiles are deleted,
   * We dont need to generate yamlChangeSets, as they have already been deleted as a part of deletion of entire service
   * folder. Also, as service is deleted, we wont be able to find this service in db, which will result into NPE.
   *
   * @param accountId
   * @param entity
   * @return
   */
  private boolean parentEntityExists(String accountId, Object entity) {
    if (entity instanceof Application) {
      return true;
    }

    try {
      String yamlFilePath = yamlHelper.getYamlPathForEntity(entity);

      String appName = yamlHelper.getAppName(yamlFilePath);
      String appId = null;
      UuidAware uuidAware = null;
      if (isNotBlank(appName)) {
        uuidAware = yamlHelper.getApp(accountId, yamlFilePath);
        if (uuidAware == null) {
          return false;
        }
        appId = uuidAware.getUuid();
      } else {
        // This is shared entity
        return true;
      }

      String serviceName = yamlHelper.getServiceName(yamlFilePath);
      if (isNotBlank(serviceName)) {
        uuidAware = yamlHelper.getService(appId, yamlFilePath);
        if (uuidAware == null) {
          return false;
        }
      }

      String envName = yamlHelper.getEnvironmentName(yamlFilePath);
      if (isNotBlank(envName)) {
        uuidAware = yamlHelper.getEnvironment(appId, yamlFilePath);
        if (uuidAware == null) {
          return false;
        }
      }
    } catch (Exception e) {
      // In case we failed to determine, just return true and try to push this delete change to GIT.
      // This is just a safegaurd to see, git sync is not affected duw to any issue in determining
      // parentEntityExists.
      return true;
    }

    return true;
  }

  public void pushYamlChangeSet(String accountId, String appId, ChangeType changeType, boolean syncFromGit) {
    if (syncFromGit) {
      return;
    }

    executorService.submit(() -> {
      try {
        yamlChangeSetHelper.defaultVariableChangeSet(accountId, appId, changeType);
      } catch (Exception e) {
        logger.error(format("Exception in pushing yaml change set for account %s", accountId), e);
      }
    });
  }

  private <R, T> void logYamlPushRequestInfo(String accountId, R helperEntity, T entity) {
    logger.info(format("%s accountId %s, entity %s, entityId %s, helperEntityId %s", YAML_PUSH_SERVICE_LOG, accountId,
        entity.getClass().getSimpleName(), ((UuidAware) entity).getUuid(), ((UuidAware) helperEntity).getUuid()));
  }

  private <T> void logYamlPushRequestInfo(String accountId, T oldEntity, T newEntity, Type type) {
    StringBuilder builder = new StringBuilder(50);
    builder.append(YAML_PUSH_SERVICE_LOG + " accountId " + accountId);

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

    logger.info(builder.toString());
  }
}