package software.wings.service.intfc;

import software.wings.beans.*;
import software.wings.utils.HostFileHelper;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.File;
import java.io.InputStream;

public interface InfraService {
  PageResponse<Infra> listInfra(String envID, PageRequest<Infra> pageRequest);

  public Infra createInfra(Infra infra, String envID);

  public PageResponse<Host> listHosts(PageRequest<Host> req);

  public Host getHost(String infraID, String hostID);

  public Host createHost(String infraID, Host host);

  public Host updateHost(String infraID, Host host);

  public Tag createTag(String envID, Tag tag);

  public Host applyTag(String hostID, String tagID);

  Integer importHosts(String infraID, InputStream uploadedInputStream, HostFileType sourceType);

  File exportHosts(String infraID, HostFileType fileType);
}
