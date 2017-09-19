package software.wings.service.impl.yaml;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.utils.Validator;
import software.wings.yaml.gitSync.YamlGitSync;
import software.wings.yaml.gitSync.YamlGitSync.Type;

import javax.inject.Inject;

public class YamlGitSyncServiceImpl implements YamlGitSyncService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  /**
   * Gets the yaml git sync info by uuid
   *
   * @param uuid the uuid
   * @return the rest response
   */
  public YamlGitSync get(String uuid) {
    YamlGitSync yamlGitSync = wingsPersistence.get(YamlGitSync.class, uuid);

    if (yamlGitSync == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "YamlGitSync for uuid: " + uuid + " doesn't exist!");
    }

    return yamlGitSync;
  }

  /**
   * Gets the yaml git sync info by object type and entitytId
   *
   * @param type the object type
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @return the rest response
   */
  public YamlGitSync get(Type type, String entityId, @NotEmpty String accountId) {
    YamlGitSync yamlGitSync = wingsPersistence.createQuery(YamlGitSync.class)
                                  .field("accountId")
                                  .equal(accountId)
                                  .field("entityId")
                                  .equal(entityId)
                                  .field("type")
                                  .equal(type.name())
                                  .get();

    return yamlGitSync;
  }

  @Override
  public boolean exist(@NotEmpty String type, @NotEmpty String entityId, @NotEmpty String accountId) {
    return wingsPersistence.createQuery(YamlGitSync.class)
               .field("accountId")
               .equal(accountId)
               .field("entityId")
               .equal(entityId)
               .field("type")
               .equal(type)
               .getKey()
        != null;
  }

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param accountId the account id
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  public YamlGitSync save(String accountId, YamlGitSync ygs) {
    Validator.notNullCheck("accountId", ygs.getAccountId());

    // check if it already exists
    if (exist(ygs.getType().name(), ygs.getEntityId(), accountId)) {
      // do update instead
      return update(ygs.getEntityId(), accountId, ygs);
    }

    YamlGitSync yamlGitSync = wingsPersistence.saveAndGet(YamlGitSync.class, ygs);

    return get(yamlGitSync.getUuid());
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @param ygs the yamlGitSync info
   * @return the rest response
   */
  public YamlGitSync update(String entityId, String accountId, YamlGitSync ygs) {
    // check if it already exists
    if (exist(ygs.getType().name(), ygs.getEntityId(), accountId)) {
      YamlGitSync yamlGitSync = get(ygs.getType(), ygs.getEntityId(), accountId);

      Query<YamlGitSync> query =
          wingsPersistence.createQuery(YamlGitSync.class).field(ID_KEY).equal(yamlGitSync.getUuid());
      UpdateOperations<YamlGitSync> operations = wingsPersistence.createUpdateOperations(YamlGitSync.class)
                                                     .set("type", ygs.getType())
                                                     .set("enabled", ygs.isEnabled())
                                                     .set("url", ygs.getUrl())
                                                     .set("rootPath", ygs.getRootPath())
                                                     .set("sshKey", ygs.getSshKey())
                                                     .set("passphrase", ygs.getPassphrase())
                                                     .set("syncMode", ygs.getSyncMode());

      wingsPersistence.update(query, operations);
      return wingsPersistence.get(YamlGitSync.class, yamlGitSync.getUuid());
    }

    return null;
  }
}
