package software.wings.service.impl;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Tag;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfraService;

import javax.inject.Inject;

@Singleton
public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Infra> listInfra(String envId, PageRequest<Infra> req) {
    return wingsPersistence.query(Infra.class, req);
  }

  @Override
  public Infra createInfra(Infra infra, String envId) {
    infra.setEnvId(envId);
    return wingsPersistence.saveAndGet(Infra.class, infra);
  }

  @Override
  public PageResponse<Host> listHosts(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host getHost(String infraId, String hostId) {
    return wingsPersistence.get(Host.class, hostId);
  }

  @Override
  public Host createHost(String infraId, Host host) {
    host.setApplicationId(infraId);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Host updateHost(String infraId, Host host) {
    host.setInfraId(infraId);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Tag createTag(String envId, Tag tag) {
    tag.setEnvId(envId);
    return wingsPersistence.saveAndGet(Tag.class, tag);
  }

  @Override
  public Host applyTag(String hostId, String tagId) {
    Tag tag = wingsPersistence.get(Tag.class, tagId);
    Host host = wingsPersistence.get(Host.class, hostId);
    UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).add("tags", tag);
    wingsPersistence.update(host, updateOp);
    return wingsPersistence.get(Host.class, hostId);
  }
}
