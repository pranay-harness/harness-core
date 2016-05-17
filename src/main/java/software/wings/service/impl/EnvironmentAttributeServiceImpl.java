package software.wings.service.impl;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.EnvironmentAttribute;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentAttributeService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/17/16.
 */
public class EnvironmentAttributeServiceImpl implements EnvironmentAttributeService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<EnvironmentAttribute> list(PageRequest<EnvironmentAttribute> req) {
    return wingsPersistence.query(EnvironmentAttribute.class, req);
  }

  @Override
  public EnvironmentAttribute save(EnvironmentAttribute envVar) {
    return wingsPersistence.saveAndGet(EnvironmentAttribute.class, envVar);
  }

  @Override
  public EnvironmentAttribute get(String appId, String envId, String varId) {
    return wingsPersistence.get(EnvironmentAttribute.class, varId);
  }

  @Override
  public EnvironmentAttribute update(EnvironmentAttribute envVar) {
    wingsPersistence.updateFields(EnvironmentAttribute.class, envVar.getUuid(),
        ImmutableMap.of("name", envVar.getName(), "value", envVar.getValue()));
    return wingsPersistence.get(EnvironmentAttribute.class, envVar.getUuid());
  }

  @Override
  public void delete(String appId, String envId, String varId) {
    wingsPersistence.delete(EnvironmentAttribute.class, varId);
  }
}
