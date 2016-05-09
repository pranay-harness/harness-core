package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.TagType.HierarchyTagName;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.TagType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 4/1/16.
 */
@Singleton
public class EnvironmentServiceImpl implements EnvironmentService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<Environment> list(String appId) {
    Application application = wingsPersistence.createQuery(Application.class)
                                  .field(ID_KEY)
                                  .equal(appId)
                                  .retrievedFields(true, "environments")
                                  .get();
    return application.getEnvironments();
  }

  @Override
  public Environment get(String appId, String envName) {
    return null;
  }

  @Override
  public Environment save(String appId, Environment environment) {
    Environment savedEnv = wingsPersistence.saveAndGet(Environment.class, environment);
    wingsPersistence.save(new TagType(HierarchyTagName, savedEnv.getUuid()));

    UpdateOperations<Application> updateOperations =
        wingsPersistence.createUpdateOperations(Application.class).add("environments", savedEnv);
    Query<Application> updateQuery = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(appId);
    wingsPersistence.update(updateQuery, updateOperations);
    return savedEnv;
  }
}
