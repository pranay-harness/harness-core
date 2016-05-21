package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Infra.InfraBuilder.anInfra;
import static software.wings.beans.Infra.InfraType.STATIC;
import static software.wings.beans.Tag.TagBuilder.aTag;

import com.google.common.collect.ImmutableMap;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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
  public Environment save(Environment env) {
    env = wingsPersistence.saveAndGet(Environment.class, env);

    wingsPersistence.save(aTag()
                              .withAppId(env.getAppId())
                              .withEnvId(env.getUuid())
                              .withName(env.getName())
                              .withDescription(env.getName())
                              .withRootTag(true)
                              .build());
    wingsPersistence.save(
        anInfra().withAppId(env.getAppId()).withEnvId(env.getUuid()).withInfraType(STATIC).build()); // FIXME: stopgap
                                                                                                     // for Alpha

    UpdateOperations<Application> updateOperations =
        wingsPersistence.createUpdateOperations(Application.class).add("environments", env);
    Query<Application> updateQuery =
        wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(env.getAppId());
    wingsPersistence.update(updateQuery, updateOperations);
    return env;
  }

  @Override
  public Environment get(String appId, String envId) {
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
