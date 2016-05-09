package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.TagType.HierarchyTagName;

import com.google.common.collect.ImmutableMap;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.TagType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 4/1/16.
 */
@Singleton
public class EnvironmentServiceImpl implements EnvironmentService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Environment> list(PageRequest<Environment> request) {
    return wingsPersistence.query(Environment.class, request);
  }

  @Override
  public Environment save(Environment environment) {
    Environment savedEnv = wingsPersistence.saveAndGet(Environment.class, environment);
    wingsPersistence.save(new TagType(HierarchyTagName, savedEnv.getUuid()));

    UpdateOperations<Application> updateOperations =
        wingsPersistence.createUpdateOperations(Application.class).add("environments", savedEnv);
    Query<Application> updateQuery =
        wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(environment.getAppId());
    wingsPersistence.update(updateQuery, updateOperations);
    return savedEnv;
  }

  @Override
  public Environment get(String envId) {
    return wingsPersistence.get(Environment.class, envId);
  }

  @Override
  public Environment update(Environment environment) {
    wingsPersistence.updateFields(Environment.class, environment.getUuid(),
        ImmutableMap.of("name", environment.getName(), "description", environment.getDescription()));
    return wingsPersistence.get(Environment.class, environment.getUuid());
  }

  @Override
  public void delete(String envId) {
    wingsPersistence.delete(Environment.class, envId);
  }
}
