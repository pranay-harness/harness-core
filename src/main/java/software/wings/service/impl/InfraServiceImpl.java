package software.wings.service.impl;

import javax.inject.Inject;

import com.google.inject.Singleton;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.*;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfraService;
import software.wings.utils.HostFileHelper;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.*;
import java.util.List;

@Singleton
public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Infra> listInfra(String envID, PageRequest<Infra> req) {
    return wingsPersistence.query(Infra.class, req);
  }

  @Override
  public Infra createInfra(Infra infra, String envID) {
    infra.setEnvID(envID);
    return wingsPersistence.saveAndGet(Infra.class, infra);
  }

  @Override
  public PageResponse<Host> listHosts(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host getHost(String infraID, String hostID) {
    return wingsPersistence.get(Host.class, hostID);
  }

  @Override
  public Host createHost(String infraID, Host host) {
    host.setInfraID(infraID);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Host updateHost(String infraID, Host host) {
    host.setInfraID(infraID);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Tag createTag(String envID, Tag tag) {
    tag.setEnvID(envID);
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
  public Integer importHosts(String infraID, InputStream inputStream, HostFileType fileType) {
    Infra infra = wingsPersistence.get(Infra.class, infraID); // TODO: validate infra
    List<Host> hosts = HostFileHelper.parseHosts(inputStream, infraID, fileType);
    List<String> IDs = wingsPersistence.save(hosts);
    return IDs.size();
  }

  @Override
  public File exportHosts(String infraID, HostFileType fileType) {
    List<Host> hosts = wingsPersistence.createQuery(Host.class).field("infraID").equal(infraID).asList();
    return HostFileHelper.createHostsFile(hosts, fileType);
  }
}
