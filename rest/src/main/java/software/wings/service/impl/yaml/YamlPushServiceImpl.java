package software.wings.service.impl.yaml;

import static io.harness.govern.Switch.unhandled;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Event.Type;
import software.wings.beans.Service;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.utils.Validator;

import java.util.concurrent.ExecutorService;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class YamlPushServiceImpl implements YamlPushService {
  private static final Logger logger = LoggerFactory.getLogger(YamlPushServiceImpl.class);

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
            pushYamlChangeSetOnCreate(accountId, newEntity);
            break;

          case UPDATE:
            validateUpdate(oldEntity, newEntity);
            pushYamlChangeSetOnUpdate(accountId, oldEntity, newEntity, isRename);
            break;

          case DELETE:
            validateDelete(oldEntity, newEntity);
            pushYamlChangeSetOnDelete(accountId, oldEntity);
            break;

          default:
            unhandled(type);
        }
      } catch (Exception e) {
        logger.error("Exception in pushing yaml change set {}", e);
      }
    });
  }

  @Override
  public <T> void pushYamlChangeSet(String accountId, Service service, T entity, Type type, boolean syncFromGit) {
    if (syncFromGit) {
      return;
    }

    executorService.submit(() -> {
      try {
        notNullCheck("entity", entity);
        notNullCheck("service", service);
        notNullCheck("accountId", accountId);

        switch (type) {
          case CREATE:
            yamlChangeSetHelper.entityYamlChangeSet(accountId, service, entity, ChangeType.ADD);
            break;

          case UPDATE:
            yamlChangeSetHelper.entityYamlChangeSet(accountId, service, entity, ChangeType.MODIFY);
            break;

          default:
            unhandled(type);
        }
      } catch (Exception e) {
        logger.error("Exception in pushing yaml change set {}", e);
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

  public void pushYamlChangeSet(String accountId, String appId, ChangeType changeType) {
    executorService.submit(() -> {
      try {
        yamlChangeSetHelper.defaultVariableChangeSet(accountId, appId, changeType);
      } catch (Exception e) {
        logger.error("Exception in pushing yaml change set {}", e);
      }
    });
  }
}