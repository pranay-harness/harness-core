package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Host.HostBuilder.aHost;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HostService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostCsvFileHelper;

import java.io.File;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 5/9/16.
 */
public class HostServiceImpl implements HostService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostCsvFileHelper csvFileHelper;

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
            .put("hostName", host.getHostName())
            .put("hostConnAttr", host.getHostConnAttr())
            .put("bastionConnAttr", host.getBastionConnAttr())
            .put("tags", host.getTags())
            .build());
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public int importHosts(String appId, String infraId, BoundedInputStream inputStream) {
    Infra infra = wingsPersistence.get(Infra.class, infraId); // TODO: validate infra
    List<Host> hosts = csvFileHelper.parseHosts(infra, inputStream);
    List<String> Ids = wingsPersistence.save(hosts);
    return Ids.size();
  }

  @Override
  public List<Host> getHostsById(String appId, List<String> hostUuids) {
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  @Override
  public File exportHosts(String appId, String infraId) {
    List<Host> hosts = wingsPersistence.createQuery(Host.class).field("infraId").equal(infraId).asList();
    return csvFileHelper.createHostsFile(hosts);
  }

  @Override
  public String getInfraId(String envId, String appId) {
    return wingsPersistence.createQuery(Infra.class).field("envId").equal(envId).get().getUuid();
  }

  @Override
  public void delete(String appId, String infraId, String hostId) {
    wingsPersistence.delete(Host.class, hostId);
  }

  @Override
  public void bulkSave(Host baseHost, List<String> hostNames) {
    hostNames.forEach(hostName -> {
      save(aHost()
               .withHostName(hostName)
               .withAppId(baseHost.getAppId())
               .withInfraId(baseHost.getInfraId())
               .withHostConnAttrs(baseHost.getHostConnAttr())
               .withBastionConnAttrs(baseHost.getBastionConnAttr())
               .withTags(baseHost.getTags())
               .build());
    });
  }
}
