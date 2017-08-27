package software.wings.service.impl.dashboardStats;

import static software.wings.beans.stats.dashboard.EntitySummary.Builder.anEntitySummary;
import static software.wings.beans.stats.dashboard.EntitySummaryStats.Builder.anEntitySummaryStats;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.stats.dashboard.ArtifactSummary;
import software.wings.beans.stats.dashboard.EntitySummary;
import software.wings.beans.stats.dashboard.EntitySummaryStats;
import software.wings.beans.stats.dashboard.EnvironmentSummary;
import software.wings.beans.stats.dashboard.InstanceDetails;
import software.wings.beans.stats.dashboard.InstanceStats;
import software.wings.beans.stats.dashboard.InstanceStatsByArtifact;
import software.wings.beans.stats.dashboard.InstanceStatsByEnvironment;
import software.wings.beans.stats.dashboard.InstanceStatsByService;
import software.wings.beans.stats.dashboard.InstanceSummaryStats;
import software.wings.beans.stats.dashboard.ServiceSummary;
import software.wings.beans.stats.dashboard.ServiceSummary.Builder;
import software.wings.beans.stats.dashboard.service.CurrentActiveInstances;
import software.wings.beans.stats.dashboard.service.DeploymentHistory;
import software.wings.beans.stats.dashboard.service.PipelineExecutionHistory;
import software.wings.beans.stats.dashboard.service.ServiceInstanceDashboard;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.dashboardStats.DashboardStatisticsService;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 8/13/17
 */
@Singleton
public class DashboardStatisticsServiceImplStub implements DashboardStatisticsService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public InstanceSummaryStats getAppInstanceSummaryStats(List<String> appIds, List<String> groupByEntityTypes) {
    Map<String, List<EntitySummaryStats>> countMap = new HashMap<>();
    groupByEntityTypes.stream().forEach(
        groupByEntityType -> { countMap.put(groupByEntityType, getStubData(groupByEntityType, 30)); });

    return InstanceSummaryStats.Builder.anInstanceSummaryStats().withCountMap(countMap).withTotalCount(100L).build();
  }

  @Override
  public InstanceSummaryStats getServiceInstanceSummaryStats(String serviceId, List<String> groupByEntityTypes) {
    Map<String, List<EntitySummaryStats>> countMap = new HashMap<>();
    groupByEntityTypes.stream().forEach(
        groupByEntityType -> { countMap.put(groupByEntityType, getStubData(groupByEntityType, 20)); });

    return InstanceSummaryStats.Builder.anInstanceSummaryStats().withCountMap(countMap).withTotalCount(50L).build();
  }

  @Override
  public List<InstanceStatsByService> getAppInstanceStats(List<String> appIds) {
    List<InstanceStatsByService> instanceStatsByServiceList = new ArrayList<>(3);
    instanceStatsByServiceList.add(getInstanceStatsByService(25, "service1", "s1Id"));
    instanceStatsByServiceList.add(getInstanceStatsByService(40, "service2", "s2Id"));
    instanceStatsByServiceList.add(getInstanceStatsByService(30, "service3", "s3Id"));
    return instanceStatsByServiceList;
  }

  @Override
  public ServiceInstanceDashboard getServiceInstanceDashboard(String serviceId) {
    return ServiceInstanceDashboard.Builder.aServiceInstanceDashboard()
        .withCurrentActiveInstancesList(getCurrentActiveInstancesList())
        .withDeploymentHistoryList(getDeploymentHistoryList())
        .withPipelineExecutionHistoryList(getPipelineExecutionHistoryList())
        .build();
  }

  private List<PipelineExecutionHistory> getPipelineExecutionHistoryList() {
    return Lists.newArrayList(getPipelineExecutionHistory("artifact1", "artifact1Id"),
        getPipelineExecutionHistory("artifact2", "artifact2Id"),
        getPipelineExecutionHistory("artifact3", "artifact3Id"));
  }

  private PipelineExecutionHistory getPipelineExecutionHistory(String artifactName, String artifactId) {
    return PipelineExecutionHistory.Builder.aPipelineExecutionHistory()
        .withArtifact(getArtifactSummary(artifactName, artifactId))
        .withEndTime(new Date())
        .withEnvironmentList(getEnvironmentList())
        .withPipeline(getPipeline())
        .withStartTime(new Date())
        .withStatus("SUCCESS")
        .build();
  }

  private EntitySummary getPipeline() {
    return getEntitySummary("pipeline1", "pipeline1Id", "PIPELINE");
  }

  private List<EntitySummary> getEnvironmentList() {
    return Lists.newArrayList(getEntitySummary("env1", "env1Id", "ENVIRONMENT"),
        getEntitySummary("env2", "env2Id", "ENVIRONMENT"), getEntitySummary("env3", "env3Id", "ENVIRONMENT"));
  }

  private List<DeploymentHistory> getDeploymentHistoryList() {
    return Lists.newArrayList(getDeploymentHistory("workflow1", "workflow1Id", "WORKFLOW"),
        getDeploymentHistory("workflow2", "workflow2Id", "WORKFLOW"),
        getDeploymentHistory("workflow3", "workflow3Id", "WORKFLOW"));
  }

  private DeploymentHistory getDeploymentHistory(String workflow1, String workflow1Id, String type) {
    return DeploymentHistory.Builder.aDeploymentHistory()
        .withArtifact(getArtifactSummary("artifact1", "artifact1Id"))
        .withDeployedAt(new Date())
        .withInstanceCount(20)
        .withPipeline(getPipeline())
        .withServiceInfra(getEntitySummary("serviceInfra1", "serviceInfra1Id", "SERVICE_INFRA"))
        .withStatus("SUCCESS")
        .withTriggeredBy(getEntitySummary("user1", "user1Id", "USER"))
        .withWorkflow(getEntitySummary(workflow1, workflow1Id, type))
        .build();
  }

  private List<CurrentActiveInstances> getCurrentActiveInstancesList() {
    return Lists.newArrayList(getCurrentActiveInstances("env1", "env1Id", "ENVIRONMENT"),
        getCurrentActiveInstances("env2", "env2Id", "ENVIRONMENT"),
        getCurrentActiveInstances("env3", "env3Id", "ENVIRONMENT"));
  }

  private CurrentActiveInstances getCurrentActiveInstances(String name, String id, String type) {
    return CurrentActiveInstances.Builder.aCurrentActiveInstances()
        .withArtifact(getArtifactSummary("artifact1", "artifact1Id"))
        .withDeployedAt(new Date())
        .withEnvironment(getEntitySummary(name, id, type))
        .withInstanceCount(40)
        .withServiceInfra(getEntitySummary("serviceInfra1", "serviceInfra1Id", "SERVICE_INFRA"))
        .build();
  }

  private InstanceStatsByService getInstanceStatsByService(int totalCount, String serviceName, String serviceId) {
    return InstanceStatsByService.Builder.anInstanceStatsByService()
        .withInstanceStatsByEnvList(getInstanceStatsByEnvList())
        .withServiceSummary(getServiceSummary(serviceName, serviceId))
        .withTotalCount(totalCount)
        .build();
  }

  private ServiceSummary getServiceSummary(String serviceName, String serviceId) {
    EntitySummary appSummary = getEntitySummary("appName", "app1", "Application");
    Builder builder = Builder.aServiceSummary().withAppSummary(appSummary);
    builder.withId(serviceId);
    builder.withName(serviceName);
    builder.withType("SERVICE");
    return builder.build();
  }

  private EntitySummary getEntitySummary(String name, String id, String type) {
    return EntitySummary.Builder.anEntitySummary().withType(type).withId(id).withName(name).build();
  }

  private ArtifactSummary getArtifactSummary(String name, String id) {
    ArtifactSummary.Builder builder =
        ArtifactSummary.Builder.anArtifactSummary().withBuildNo("1234").withArtifactSourceName("https://artifact1");
    builder.withType("ARTIFACT").withId(id).withName(name).build();
    return builder.build();
  }

  private List<InstanceStatsByEnvironment> getInstanceStatsByEnvList() {
    InstanceStatsByEnvironment instanceStatsByEnv1 = getInstanceStatsByEnv("env1", "env1Id");
    InstanceStatsByEnvironment instanceStatsByEnv2 = getInstanceStatsByEnv("env2", "env2Id");
    InstanceStatsByEnvironment instanceStatsByEnv3 = getInstanceStatsByEnv("env3", "env3Id");

    return Lists.newArrayList(instanceStatsByEnv1, instanceStatsByEnv2, instanceStatsByEnv3);
  }

  private InstanceStatsByEnvironment getInstanceStatsByEnv(String envName, String envId) {
    EnvironmentSummary.Builder builder = EnvironmentSummary.Builder.anEnvironmentSummary().withProd(true);
    builder.withId(envId).withName(envName).withType("ENVIRONMENT");
    return InstanceStatsByEnvironment.Builder.anInstanceStatsByEnvironment()
        .withInstanceStatsByArtifactList(getInstanceByArtifactList())
        .withEnvironmentSummary(builder.build())
        .build();
  }

  private List<InstanceStatsByArtifact> getInstanceByArtifactList() {
    EntitySummary entitySummary1 = getArtifactSummary("artifact1", "artifact1Id");
    InstanceStatsByArtifact instanceStatsByArtifact1 = getInstanceStatsByArtifact(entitySummary1);

    EntitySummary entitySummary2 = getArtifactSummary("artifact2", "artifact2Id");
    InstanceStatsByArtifact instanceStatsByArtifact2 = getInstanceStatsByArtifact(entitySummary2);

    return Lists.newArrayList(instanceStatsByArtifact1, instanceStatsByArtifact2);
  }

  private InstanceStatsByArtifact getInstanceStatsByArtifact(EntitySummary entitySummary) {
    InstanceStatsByArtifact.Builder builder =
        InstanceStatsByArtifact.Builder.anInstanceStatsByArtifact().withEntitySummary(entitySummary);
    builder.withInstanceStats(getInstanceStats());
    return builder.build();
  }

  private InstanceStats getInstanceStats() {
    return InstanceStats.Builder.anInstanceSummaryStats()
        .withTotalCount(23)
        .withEntitySummaryList(getInstanceEntitySummaryList())
        .build();
  }

  private List<EntitySummary> getInstanceEntitySummaryList() {
    EntitySummary instance1 = getEntitySummary("instance1", "instance1Id", "INSTANCE");
    EntitySummary instance2 = getEntitySummary("instance2", "instance2Id", "INSTANCE");
    EntitySummary instance3 = getEntitySummary("instance3", "instance3Id", "INSTANCE");
    return Lists.newArrayList(instance1, instance2, instance3);
  }

  private List<EntitySummaryStats> getStubData(String entityType, int count) {
    int counter = 1;
    List<EntitySummaryStats> list = Lists.newArrayList();
    while (counter <= 3) {
      list.add(getStubEntitySummaryStats(entityType, counter++, count));
    }
    return list;
  }

  private EntitySummaryStats getStubEntitySummaryStats(String entityType, int counter, int count) {
    EntitySummary entitySummary =
        anEntitySummary().withType(entityType).withName(entityType + counter).withId(entityType + counter).build();
    return anEntitySummaryStats().withCount(count).withEntitySummary(entitySummary).build();
  }

  @Override
  public InstanceDetails getInstanceDetails(String instanceId) {
    // yet to be implemented
    return null;
  }
}
