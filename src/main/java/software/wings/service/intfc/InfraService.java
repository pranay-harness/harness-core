package software.wings.service.intfc;

import software.wings.beans.*;

public interface InfraService {
  public PageResponse<Environment> listEnvironments(PageRequest<Environment> req);
  public Environment getEnvironments(String applicationId, String envName);
  public Environment createEnvironment(String applicationId, Environment environment);

  public PageResponse<Host> listHosts(PageRequest<Host> req);
  public Host getHost(String hostUuid);
  public Host createHost(String applicationId, Host host);

  public Tag createTag(Tag tag);
  public Host applyTag(String hostID, String tagID);

  public PageResponse<HostInstanceMapping> listHostInstanceMapping(PageRequest<HostInstanceMapping> pageRequest);
  public HostInstanceMapping createHostInstanceMapping(String applicationId, HostInstanceMapping hostInstanceMapping);
}
