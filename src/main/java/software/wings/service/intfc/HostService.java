package software.wings.service.intfc;

import software.wings.beans.Host;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.File;
import java.io.InputStream;

/**
 * Created by anubhaw on 5/9/16.
 */
public interface HostService {
  public PageResponse<Host> list(PageRequest<Host> req);

  public Host get(String appId, String infraId, String hostId);

  public Host save(Host host);

  public Host update(Host host);

  Host tag(String appId, String infraId, String hostId, String tagId);

  Integer importHosts(String appId, String infraId, InputStream uploadedInputStream, HostFileType sourceType);

  File exportHosts(String appId, String infraId, HostFileType fileType);

  String getInfraId(String envId, String appId);
}
