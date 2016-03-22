package software.wings.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

@Entity(value = "infra-meta", noClassnameStored = true)
public class InfraMetaData {
  @Id private ObjectId id;

  @Indexed @Reference(idOnly = true) private Environment environment;

  private List<DataCenter> dataCenters = new ArrayList<>();
  private List<OperationalZone> operationalZones = new ArrayList<>();
  private List<ServiceInstance> instances = new ArrayList<>();

  public Environment getEnvironment() {
    return environment;
  }
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
  public List<DataCenter> getDataCenters() {
    return dataCenters;
  }
  public void setDataCenters(List<DataCenter> dataCenters) {
    this.dataCenters = dataCenters;
  }
  public List<OperationalZone> getOperationalZones() {
    return operationalZones;
  }
  public void setOperationalZones(List<OperationalZone> operationalZones) {
    this.operationalZones = operationalZones;
  }
  public List<ServiceInstance> getInstances() {
    return instances;
  }
  public void setInstances(List<ServiceInstance> instances) {
    this.instances = instances;
  }
}
