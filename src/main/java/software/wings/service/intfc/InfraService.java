package software.wings.service.intfc;

import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.File;
import java.io.InputStream;

public interface InfraService {
  PageResponse<Infra> list(String envId, PageRequest<Infra> pageRequest);

  public Infra save(Infra infra, String envId);

  public PageResponse<Host> listHosts(PageRequest<Host> req);

  public Host getHost(String infraId, String hostId);

  public Host createHost(String infraId, Host host);

  public Host updateHost(String infraId, Host host);

  Host tagHost(String hostId, String tagId);

  Integer importHosts(String infraId, InputStream uploadedInputStream, HostFileType sourceType);

  File exportHosts(String infraId, HostFileType fileType);
}
