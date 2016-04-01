package software.wings.service.impl;

import javax.inject.Inject;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.*;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfraService;

@Singleton
public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Host> listHosts(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host getHost(String appID, String hostUuid) {
    return wingsPersistence.get(Host.class, hostUuid);
  }

  @Override
  public Host createHost(String applicationId, Host host) {
    host.setApplicationId(applicationId);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Tag createTag(Tag tag) {
    return wingsPersistence.saveAndGet(Tag.class, tag);
  }

  @Override
  public Host applyTag(String hostID, String tagID) {
    Tag tag = wingsPersistence.get(Tag.class, tagID);
    Host host = wingsPersistence.get(Host.class, hostID);
    UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).add("tags", tag);
    wingsPersistence.update(host, updateOp);
    return wingsPersistence.get(Host.class, hostID);
  }

  @Override
  public HostInstanceMapping createHostInstanceMapping(String applicationId, HostInstanceMapping hostInstanceMapping) {
    hostInstanceMapping.setApplicationId(applicationId);
    return wingsPersistence.saveAndGet(HostInstanceMapping.class, hostInstanceMapping);
  }

  @Override
  public PageResponse<HostInstanceMapping> listHostInstanceMapping(PageRequest<HostInstanceMapping> pageRequest) {
    return wingsPersistence.query(HostInstanceMapping.class, pageRequest);
  }
}
