package software.wings.service.intfc;

import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Tag;

public interface InfraService {
  PageResponse<Infra> listInfra(String envId, PageRequest<Infra> pageRequest);

  public Infra createInfra(Infra infra, String envId);

  public PageResponse<Host> listHosts(PageRequest<Host> req);

  public Host getHost(String infraId, String hostId);

  public Host createHost(String infraId, Host host);

  public Host updateHost(String infraId, Host host);

  public Tag createTag(String envId, Tag tag);

  public Host applyTag(String hostId, String tagID);
}
