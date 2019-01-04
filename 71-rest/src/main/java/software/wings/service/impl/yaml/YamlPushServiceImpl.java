package software.wings.service.impl.yaml;

import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.Event.Type;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.utils.Validator;

import java.util.concurrent.ExecutorService;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class YamlPushServiceImpl implements YamlPushService {
  private static final Logger logger = LoggerFactory.getLogger(YamlPushServiceImpl.class);
  private static final String YAML_PUSH_SERVICE_LOG = "YAML_PUSH_SERVICE_LOG";

  @Inject private YamlChangeSetHelper yamlChangeSetHelper;
  @Inject private ExecutorService executorService;

  @Override
  public <T> void pushYamlChangeSet(
      String accountId, T oldEntity, T newEntity, Type type, boolean syncFromGit, boolean isRename) {
    if (syncFromGit) {
      return;
    }

    executorService.submit(() -> {
      try {
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
    yamlChangeSetHelper.entityYamlChangeSet(accountId, entity, ChangeType.DELETE);
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
        entity.getClass().getSimpleName(), ((Base) entity).getUuid(), ((Base) helperEntity).getUuid()));
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
            .append(((Base) newEntity).getUuid());
        break;

      case UPDATE:
        builder.append(", ")
            .append(type.name())
            .append(", entity ")
            .append(oldEntity.getClass().getSimpleName())
            .append(", oldEntityId ")
            .append(((Base) oldEntity).getUuid())
            .append(", newEntityId ")
            .append(((Base) newEntity).getUuid());
        break;

      case DELETE:
        builder.append(", ")
            .append(type.name())
            .append(", entity ")
            .append(oldEntity.getClass().getSimpleName())
            .append(", entityId ")
            .append(((Base) oldEntity).getUuid());

        break;

      default:
        unhandled(type);
    }

    logger.info(builder.toString());
  }
}