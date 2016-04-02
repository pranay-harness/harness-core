package software.wings.service.intfc;

import software.wings.beans.*;

public interface InfraService {
  public PageResponse<Host> listHosts(PageRequest<Host> req);
  public Host getHost(String appID, String hostUuid);
  public Host createHost(String applicationId, Host host);

  public Tag createTag(Tag tag);
  public Host applyTag(String hostID, String tagID);
}
