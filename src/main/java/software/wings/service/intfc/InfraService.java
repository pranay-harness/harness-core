package software.wings.service.intfc;

import software.wings.beans.*;

public interface InfraService {
  public PageResponse<Environment> listEnvironments(PageRequest<Environment> req);
  public Environment getEnvironments(String applicationId, String envName);
  public Environment createEnvironment(String applicationId, Environment environment);

  public PageResponse<DataCenter> listDataCenters(PageRequest<DataCenter> req);
  public DataCenter getDataCenter(String applicationId, String envName, String dcName);
  public DataCenter createDataCenter(String applicationId, String envName, DataCenter DataCenter);

  public PageResponse<OperationalZone> listOperationalZones(PageRequest<OperationalZone> req);
  public OperationalZone getOperationalZone(String applicationId, String envName, String dcName, String ozName);
  public OperationalZone createOperationalZone(OperationalZone OperationalZone);

  public PageResponse<Phase> listPhases(PageRequest<Phase> req);
  public Phase getPhase(String applicationId, String envName, String compName, String phaseName);
  public Phase createPhase(Phase Phase);

  public PageResponse<Host> listHosts(PageRequest<Host> req);
  public Host getHost(String applicationId, String hostUuid);
  public Host createHost(String applicationId, Host host);

  public Tag createTag(Tag tag);
  public Host applyTag(String hostID, String tagID);

  public PageResponse<HostInstanceMapping> listHostInstanceMapping(PageRequest<HostInstanceMapping> pageRequest);
  public HostInstanceMapping createHostInstanceMapping(String applicationId, HostInstanceMapping hostInstanceMapping);
}
