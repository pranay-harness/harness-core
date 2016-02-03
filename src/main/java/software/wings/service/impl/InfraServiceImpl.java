package software.wings.service.impl;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;

import software.wings.beans.Application;
import software.wings.beans.DataCenter;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.HostInstanceMapping;
import software.wings.beans.OperationalZone;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.Phase;
import software.wings.dl.MongoHelper;
import software.wings.service.intfc.InfraService;

public class InfraServiceImpl implements InfraService {
  private Datastore datastore;

  public InfraServiceImpl(Datastore datastore) {
    this.datastore = datastore;
  }

  @Override
  public PageResponse<Environment> listEnvironments(PageRequest<Environment> req) {
    return MongoHelper.queryPageRequest(datastore, Environment.class, req);
  }

  @Override
  public Environment getEnvironments(String applicationId, String envName) {
    return null;
  }

  @Override
  public Environment createEnvironment(String applicationId, Environment environment) {
    environment.setApplicationId(applicationId);
    Key<Environment> key = datastore.save(environment);
    return datastore.get(Environment.class, key.getId());
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
    return MongoHelper.queryPageRequest(datastore, Host.class, req);
  }

  @Override
  public Host getHost(String applicationId, String hostUuid) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Host createHost(String applicationId, Host host) {
    host.setApplicationId(applicationId);
    Key<Host> key = datastore.save(host);
    return datastore.get(Host.class, key.getId());
  }

  @Override
  public HostInstanceMapping createHostInstanceMapping(String applicationId, HostInstanceMapping hostInstanceMapping) {
    hostInstanceMapping.setApplicationId(applicationId);
    Key<HostInstanceMapping> key = datastore.save(hostInstanceMapping);
    return datastore.get(HostInstanceMapping.class, key.getId());
  }

  @Override
  public PageResponse<HostInstanceMapping> listHostInstanceMapping(PageRequest<HostInstanceMapping> pageRequest) {
    return MongoHelper.queryPageRequest(datastore, HostInstanceMapping.class, pageRequest);
  }
}
