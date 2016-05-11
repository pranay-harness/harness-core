package software.wings.service.impl;

import com.google.common.collect.ImmutableMap;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Tag;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.utils.HostFileHelper;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 5/9/16.
 */
public class HostServiceImpl implements HostService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Host> list(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host get(String appId, String infraId, String hostId) {
    return wingsPersistence.get(Host.class, hostId);
  }

  @Override
  public Host save(Host host) {
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Host update(Host host) {
    wingsPersistence.updateFields(Host.class, host.getUuid(),
        ImmutableMap.<String, Object>builder()
            .put("hostName", host.getName())
            .put("osType", host.getOsType())
            .put("connectionType", host.getConnectionType())
            .put("accessType", host.getAccessType())
            .put("tags", host.getTags())
            .build());

    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public Host tag(String appId, String infraId, String hostId, String tagId) {
    Tag tag = wingsPersistence.get(Tag.class, tagId);
    Host host = wingsPersistence.get(Host.class, hostId);
    UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).add("tags", tag);
    wingsPersistence.update(host, updateOp);
    return wingsPersistence.get(Host.class, hostId);
  }

  @Override
  public Integer importHosts(String appId, String infraId, InputStream inputStream, HostFileType fileType) {
    Infra infra = wingsPersistence.get(Infra.class, infraId); // TODO: validate infra
    List<Host> hosts = HostFileHelper.parseHosts(inputStream, appId, infraId, fileType);
    List<String> IDs = wingsPersistence.save(hosts);
    return IDs.size();
  }

  @Override
  public File exportHosts(String appId, String infraId, HostFileType fileType) {
    List<Host> hosts = wingsPersistence.createQuery(Host.class).field("infraID").equal(infraId).asList();
    return HostFileHelper.createHostsFile(hosts, fileType);
  }

  @Override
  public String getInfraId(String envId, String appId) {
    return wingsPersistence.createQuery(Infra.class).field("envId").equal(envId).get().getUuid();
  }

  @Override
  public void delete(String appId, String infraId, String hostId) {
    wingsPersistence.delete(Host.class, hostId);
  }
}
