package software.wings.service.intfc;

import software.wings.beans.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.util.List;

/**
 * Created by anubhaw on 5/9/16.
 */
public interface HostService {
  public PageResponse<Host> list(PageRequest<Host> req);

  public Host get(String appId, String infraId, String hostId);

  public Host save(Host host);

  public Host update(Host host);

  int importHosts(Host baseHost, BoundedInputStream boundedInputStream);

  File exportHosts(String appId, String infraId);

  String getInfraId(String envId, String appId);

  void delete(String appId, String infraId, String hostId);

  void bulkSave(Host baseHost, List<String> hostNames);
}
