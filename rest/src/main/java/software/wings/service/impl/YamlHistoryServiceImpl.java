package software.wings.service.impl;

import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.YamlHistoryService;
import software.wings.utils.Validator;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Yaml History Service Implementation class.
 *
 * @author bsollish
 */
@ValidateOnExecution
@Singleton
public class YamlHistoryServiceImpl implements YamlHistoryService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.YamlHistoryService#save(software.wings.beans.YamlVersion)
   */
  @Override
  public YamlVersion save(YamlVersion yv) {
    Validator.notNullCheck("accountId", yv.getAccountId());

    // get highest version in use
    int highestVersion = getHighestVersion(yv.getEntityId(), yv.getType());

    yv.setVersion(highestVersion + 1);

    // TODO - not sure how we might want to check for duplicates (?)
    // YamlVersion yamlVersion = Validator.duplicateCheck(() -> wingsPersistence.saveAndGet(YamlVersion.class, yv),
    // "entityId", yv.getEntityId());
    YamlVersion yamlVersion = wingsPersistence.saveAndGet(YamlVersion.class, yv);

    return get(yamlVersion.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.YamlHistoryService#get(java.lang.String)
   */
  @Override
  public YamlVersion get(String uuid) {
    YamlVersion yamlVersion = wingsPersistence.get(YamlVersion.class, uuid);
    if (yamlVersion == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "YamlVersion -" + uuid + " doesn't exist");
    }
    return yamlVersion;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.YamlHistoryService#get(java.lang.String)
   */
  @Override
  public List<YamlVersion> getList(String entityId, Type type) {
    List<YamlVersion> versions = new ArrayList<>();
    versions = wingsPersistence.createQuery(YamlVersion.class)
                   .field("entityId")
                   .equal(entityId)
                   .field("type")
                   .equal(type)
                   .asList();

    return versions;
  }

  @Override
  public int getHighestVersion(String entityId, Type type) {
    List<YamlVersion> versions = wingsPersistence.createQuery(YamlVersion.class)
                                     .field("entityId")
                                     .equal(entityId)
                                     .field("type")
                                     .equal(type)
                                     .order("version")
                                     .limit(1)
                                     .asList();

    int version = 0;

    if (versions.size() > 0) {
      YamlVersion yv = versions.get(0);
      version = yv.getVersion();
    }

    return version;
  }
}
