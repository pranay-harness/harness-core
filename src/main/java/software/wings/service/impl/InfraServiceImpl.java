package software.wings.service.impl;

import javax.inject.Inject;

import com.google.inject.Singleton;

import software.wings.beans.DataCenter;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.HostInstanceMapping;
import software.wings.beans.OperationalZone;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Phase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.InfraService;

@Singleton
public class InfraServiceImpl implements InfraService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<Environment> listEnvironments(PageRequest<Environment> req) {
    return wingsPersistence.query(Environment.class, req);
  }

  @Override
  public Environment getEnvironments(String applicationId, String envName) {
    return null;
  }

  @Override
  public Environment createEnvironment(String applicationId, Environment environment) {
    environment.setApplicationId(applicationId);
    return wingsPersistence.saveAndGet(Environment.class, environment);
  }

  @Override
  public PageResponse<DataCenter> listDataCenters(PageRequest<DataCenter> req) {
    return null;
  }

  @Override
  public DataCenter getDataCenter(String applicationId, String envName, String dcName) {
    return null;
  }

  @Override
  public DataCenter createDataCenter(String applicationId, String envName, DataCenter DataCenter) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PageResponse<OperationalZone> listOperationalZones(PageRequest<OperationalZone> req) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OperationalZone getOperationalZone(String applicationId, String envName, String dcName, String ozName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public OperationalZone createOperationalZone(OperationalZone OperationalZone) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PageResponse<Phase> listPhases(PageRequest<Phase> req) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Phase getPhase(String applicationId, String envName, String compName, String phaseName) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Phase createPhase(Phase Phase) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public PageResponse<Host> listHosts(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host getHost(String applicationId, String hostUuid) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Host createHost(String applicationId, Host host) {
    host.setApplicationId(applicationId);
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  @Override
  public HostInstanceMapping createHostInstanceMapping(String applicationId, HostInstanceMapping hostInstanceMapping) {
    hostInstanceMapping.setApplicationId(applicationId);
    return wingsPersistence.saveAndGet(HostInstanceMapping.class, hostInstanceMapping);
  }

  @Override
  public PageResponse<HostInstanceMapping> listHostInstanceMapping(PageRequest<HostInstanceMapping> pageRequest) {
    return wingsPersistence.query(HostInstanceMapping.class, pageRequest);
  }
}
