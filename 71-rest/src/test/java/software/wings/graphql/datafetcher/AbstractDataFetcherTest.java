package software.wings.graphql.datafetcher;

import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.ccm.setup.CECloudAccountDao;
import io.harness.ccm.setup.CEClusterDao;
import io.harness.persistence.HPersistence;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Application.Builder;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.LicenseInfo;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECluster;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.events.TestUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

public abstract class AbstractDataFetcherTest extends WingsBaseTest {
  public static final String TAG_TEAM = "TEAM";
  public static final String TAG_VALUE_TEAM1 = "TEAM1";
  public static final String TAG_VALUE_TEAM2 = "TEAM2";
  public static final String TAG_TEAM1 = "TEAM:TEAM1";
  public static final String TAG_TEAM2 = "TEAM:TEAM2";
  public static final String TAG_MODULE = "MODULE";
  public static final String TAG_VALUE_MODULE1 = "MODULE1";
  public static final String TAG_VALUE_MODULE2 = "MODULE2";
  public static final String TAG_MODULE1 = "MODULE:MODULE1";
  public static final String TAG_MODULE2 = "MODULE:MODULE2";
  public static final String TAG_ENVTYPE = "ENVTYPE";
  public static final String TAG_VALUE_PROD = "PROD";
  public static final String TAG_VALUE_NON_PROD = "NON_PROD";
  public static final String TAG_PROD = "ENVTYPE:PROD";
  public static final String TAG_NON_PROD = "ENVTYPE:NON_PROD";
  public static final String ACCOUNT1_ID = "ACCOUNT1_ID";
  public static final String APP1_ID_ACCOUNT1 = "APP1_ID_ACCOUNT1";
  public static final String SERVICE1_ID_APP1_ACCOUNT1 = "SERVICE1_ID_APP1_ACCOUNT1";
  public static final String SERVICE2_ID_APP1_ACCOUNT1 = "SERVICE2_ID_APP1_ACCOUNT1";
  public static final String ENV1_ID_APP1_ACCOUNT1 = "ENV1_ID_APP1_ACCOUNT1";
  public static final String ENV2_ID_APP1_ACCOUNT1 = "ENV2_ID_APP1_ACCOUNT1";
  public static final String INSTANCE1_SERVICE1_ENV1_APP1_ACCOUNT1 = "INSTANCE1_SERVICE1_ENV1_APP1_ACCOUNT1";
  public static final String INSTANCE2_SERVICE1_ENV1_APP1_ACCOUNT1 = "INSTANCE2_SERVICE1_ENV1_APP1_ACCOUNT1";
  public static final String INSTANCE3_SERVICE1_ENV2_APP1_ACCOUNT1 = "INSTANCE3_SERVICE1_ENV2_APP1_ACCOUNT1";
  public static final String INSTANCE4_SERVICE2_ENV2_APP1_ACCOUNT1 = "INSTANCE4_SERVICE2_ENV2_APP1_ACCOUNT1";
  public static final String APP2_ID_ACCOUNT1 = "APP2_ID_ACCOUNT1";
  public static final String SERVICE3_ID_APP2_ACCOUNT1 = "SERVICE3_ID_APP2_ACCOUNT1";
  public static final String ENV3_ID_APP2_ACCOUNT1 = "ENV3_ID_APP2_ACCOUNT1";
  public static final String ENV4_ID_APP2_ACCOUNT1 = "ENV4_ID_APP2_ACCOUNT1";
  public static final String INSTANCE5_SERVICE3_ENV3_APP2_ACCOUNT1 = "INSTANCE5_SERVICE3_ENV3_APP2_ACCOUNT1";
  public static final String INSTANCE6_SERVICE3_ENV4_APP2_ACCOUNT1 = "INSTANCE6_SERVICE3_ENV4_APP2_ACCOUNT1";
  public static final String CLOUD_PROVIDER1_ID_ACCOUNT1 = "CLOUD_PROVIDER1_ID_ACCOUNT1";
  public static final String CLOUD_PROVIDER2_ID_ACCOUNT1 = "CLOUD_PROVIDER2_ID_ACCOUNT1";
  public static final String CLOUD_PROVIDER3_ID_ACCOUNT2 = "CLOUD_PROVIDER3_ID_ACCOUNT2";
  public static final String CLOUD_SERVICE_NAME_ACCOUNT1 = "CLOUD_SERVICE_NAME_ACCOUNT1";
  public static final String WORKLOAD_NAME_ACCOUNT1 = "WORKLOAD_NAME_ACCOUNT1";
  public static final String WORKLOAD_TYPE_ACCOUNT1 = "WORKLOAD_TYPE_ACCOUNT1";
  public static final String ACCOUNT2_ID = "ACCOUNT2_ID";
  public static final String APP3_ID_ACCOUNT2 = "APP3_ID_ACCOUNT2";
  public static final String SERVICE4_ID_APP3_ACCOUNT2 = "SERVICE4_ID_APP3_ACCOUNT2";
  public static final String ENV5_ID_APP3_ACCOUNT2 = "ENV5_ID_APP3_ACCOUNT2";
  public static final String ENV6_ID_APP3_ACCOUNT2 = "ENV6_ID_APP3_ACCOUNT2";
  public static final String INSTANCE7_SERVICE4_ENV5_APP3_ACCOUNT2 = "INSTANCE7_SERVICE4_ENV5_APP3_ACCOUNT2";
  public static final String INSTANCE8_SERVICE4_ENV6_APP3_ACCOUNT2 = "INSTANCE8_SERVICE4_ENV6_APP3_ACCOUNT2";
  public static final String WORKFLOW1 = "WORKFLOW1";
  public static final String PIPELINE1 = "PIPELINE1";
  public static final String REGION1 = "REGION1";
  public static final String CLUSTER1_ID = "CLUSTER1_ID";
  public static final String CLUSTER2_ID = "5e144cbfececcf83f5b29cd5";
  public static final String CLUSTER1_NAME = "CLUSTER1_NAME";
  public static final String LAUNCH_TYPE1 = "LAUNCH_TYPE1";
  public static final String CLUSTER_TYPE1 = "CLUSTER_TYPE1";
  public static final String NAMESPACE1 = "NAMESPACE1";
  public static final String INSTANCE_TYPE1 = "INSTANCE_TYPE1";
  public static final String LABEL_NAME = "LABEL_NAME";
  public static final String LABEL_VALUE = "LABEL_VALUE";
  public static final String LABEL = "LABEL_NAME:LABEL_VALUE";
  public static final String SETTING_ID1 = "SETTING_ID1";
  public static final String QUERY1 =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, time_bucket_gapfill('1 hours',REPORTEDAT,'2009-02-12T11:19:15.233Z','2009-02-12T16:19:15.233Z') AS GRP_BY_TIME FROM (SELECT REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE  REPORTEDAT  >= timestamp '2009-02-12T11:19:15.233Z' AND REPORTEDAT  < timestamp '2009-02-12T16:19:15.233Z' AND ACCOUNTID = 'ACCOUNT1_ID' GROUP BY REPORTEDAT) INSTANCE_STATS GROUP BY GRP_BY_TIME ORDER BY GRP_BY_TIME";
  public static final String QUERY2 =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, time_bucket_gapfill('1 hours',REPORTEDAT,'2009-02-12T11:19:15.233Z','2009-02-12T16:19:15.233Z') AS GRP_BY_TIME, ENTITY_ID FROM (SELECT APPID AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE  APPID  = 'APP1_ID_ACCOUNT1' AND  REPORTEDAT  >= timestamp '2009-02-12T11:19:15.233Z' AND REPORTEDAT  < timestamp '2009-02-12T16:19:15.233Z' AND ACCOUNTID = 'ACCOUNT1_ID' GROUP BY ENTITY_ID, REPORTEDAT) INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME";
  public static final String QUERY3 =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, time_bucket_gapfill('1 days',REPORTEDAT,'2009-02-12T11:19:15.233Z','2009-02-12T16:19:15.233Z') AS GRP_BY_TIME, ENTITY_ID FROM (SELECT SERVICEID AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE  SERVICEID  IN ('SERVICE1_ID_APP1_ACCOUNT1','SERVICE2_ID_APP1_ACCOUNT1') AND  REPORTEDAT  >= timestamp '2009-02-12T11:19:15.233Z' AND REPORTEDAT  < timestamp '2009-02-12T16:19:15.233Z' AND ACCOUNTID = 'ACCOUNT1_ID' GROUP BY ENTITY_ID, REPORTEDAT) INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME";
  public static final String QUERY4 =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, time_bucket_gapfill('1 days',REPORTEDAT,'2009-02-12T11:19:15.233Z','2009-02-12T16:19:15.233Z') AS GRP_BY_TIME, ENTITY_ID FROM (SELECT CLOUDPROVIDERID AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE  CLOUDPROVIDERID  IN ('CLOUD_PROVIDER1_ID_ACCOUNT1','CLOUD_PROVIDER2_ID_ACCOUNT1') AND  REPORTEDAT  >= timestamp '2009-02-12T11:19:15.233Z' AND REPORTEDAT  < timestamp '2009-02-12T16:19:15.233Z' AND ACCOUNTID = 'ACCOUNT1_ID' GROUP BY ENTITY_ID, REPORTEDAT) INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME";
  public static final String QUERY5 =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, time_bucket_gapfill('1 days',REPORTEDAT,'2009-02-12T11:19:15.233Z','2009-02-12T16:19:15.233Z') AS GRP_BY_TIME, ENTITY_ID FROM (SELECT ENVID AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE  ENVID  = 'ENV1_ID_APP1_ACCOUNT1' AND  REPORTEDAT  >= timestamp '2009-02-12T11:19:15.233Z' AND REPORTEDAT  < timestamp '2009-02-12T16:19:15.233Z' AND ACCOUNTID = 'ACCOUNT1_ID' GROUP BY ENTITY_ID, REPORTEDAT) INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME";
  public static final String QUERY6 =
      "SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, time_bucket_gapfill('1 days',REPORTEDAT,'2009-02-12T11:19:15.233Z','2009-02-12T16:19:15.233Z') AS GRP_BY_TIME, ENTITY_ID FROM (SELECT APPID AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE  ENVID  IN ('ENV1_ID_APP1_ACCOUNT1','ENV3_ID_APP2_ACCOUNT1') AND  REPORTEDAT  >= timestamp '2009-02-12T11:19:15.233Z' AND REPORTEDAT  < timestamp '2009-02-12T16:19:15.233Z' AND ACCOUNTID = 'ACCOUNT1_ID' GROUP BY ENTITY_ID, REPORTEDAT) INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME";
  @Inject AccountService accountService;
  @Inject AppService appService;
  @Inject HarnessTagService harnessTagService;
  @Inject ServiceResourceService serviceResourceService;
  @Inject EnvironmentService environmentService;
  @Inject InstanceService instanceService;
  @Inject SettingsService settingsService;
  @Inject WorkflowService workflowService;
  @Inject PipelineService pipelineService;
  @Inject WorkflowExecutionService workflowExecutionService;
  @Inject ClusterRecordService clusterRecordService;
  @Inject CEClusterDao clusterDao;
  @Inject CECloudAccountDao cloudAccountDao;
  @Inject HPersistence hPersistence;
  @Inject protected TestUtils testUtils;

  public Account createAccount(String accountId, LicenseInfo licenseInfo) {
    return accountService.save(anAccount()
                                   .withCompanyName(accountId)
                                   .withAccountName(accountId)
                                   .withAccountKey("ACCOUNT_KEY")
                                   .withLicenseInfo(licenseInfo)
                                   .withUuid(accountId)
                                   .build(),
        false);
  }

  public Application createApp(String accountId, String appId, String appName, String tagKey, String tagValue) {
    Application application =
        appService.save(Builder.anApplication().name(appName).accountId(accountId).uuid(appId).build());
    setTagToEntity(tagKey, tagValue, accountId, appId, appId, EntityType.APPLICATION);
    return application;
  }

  public String createWorkflowExecution(String accountId, String appId, String workflowId) {
    WorkflowExecution workflowExecution =
        WorkflowExecution.builder().workflowId(workflowId).accountId(accountId).appId(appId).build();
    return wingsPersistence.insert(workflowExecution);
  }

  public Workflow createWorkflow(String accountId, String appId, String workflowName) {
    return workflowService.createWorkflow(WorkflowBuilder.aWorkflow()
                                              .workflowType(WorkflowType.ORCHESTRATION)
                                              .accountId(accountId)
                                              .appId(appId)
                                              .name(workflowName)
                                              .build());
  }

  public Pipeline createPipeline(String accountId, String appId, String pipelineName) {
    return pipelineService.save(Pipeline.builder().accountId(accountId).appId(appId).name(pipelineName).build());
  }

  public void setTagToEntity(
      String tagKey, String tagValue, String accountId, String appId, String entityId, EntityType entityType) {
    harnessTagService.attachTagWithoutGitPush(HarnessTagLink.builder()
                                                  .key(tagKey)
                                                  .value(tagValue)
                                                  .entityId(entityId)
                                                  .entityType(entityType)
                                                  .accountId(accountId)
                                                  .appId(appId)
                                                  .build());
  }

  public Service createService(
      String accountId, String appId, String serviceId, String serviceName, String tagKey, String tagValue) {
    Service service = serviceResourceService.save(
        Service.builder().name(serviceName).uuid(serviceId).appId(appId).accountId(accountId).build());
    setTagToEntity(tagKey, tagValue, accountId, appId, serviceId, EntityType.SERVICE);
    return service;
  }

  public Environment createEnv(
      String accountId, String appId, String envId, String envName, String tagKey, String tagValue) {
    Environment environment = environmentService.save(
        Environment.Builder.anEnvironment().name(envName).uuid(envId).appId(appId).accountId(accountId).build());
    setTagToEntity(tagKey, tagValue, accountId, appId, envId, EntityType.ENVIRONMENT);
    return environment;
  }

  public Instance createInstance(String accountId, String appId, String envId, String serviceId,
      EnvironmentType envType, String instanceId, String cloudProviderId) {
    return instanceService.save(Instance.builder()
                                    .accountId(accountId)
                                    .appId(appId)
                                    .appName(appId)
                                    .serviceId(serviceId)
                                    .serviceName(serviceId)
                                    .envId(envId)
                                    .envType(envType)
                                    .envName(envId)
                                    .instanceType(InstanceType.PHYSICAL_HOST_INSTANCE)
                                    .computeProviderId(cloudProviderId)
                                    .computeProviderName(cloudProviderId)
                                    .createdAt(System.currentTimeMillis() - 100000L)
                                    .uuid(instanceId)
                                    .build());
  }

  public void createCloudProvider(String accountId, String appId, String uuid, String name) {
    SettingValue settingValue = PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                                    .withType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                                    .build();
    SettingAttribute cloudProvider = SettingAttribute.Builder.aSettingAttribute()
                                         .withName(name)
                                         .withValue(settingValue)
                                         .withUuid(uuid)
                                         .withAccountId(accountId)
                                         .withAppId(appId)
                                         .withCategory(SettingCategory.CLOUD_PROVIDER)
                                         .build();
    settingsService.save(cloudProvider, false);
  }

  public void createCEConnector(String uuid, String accountId, String name, SettingValue settingValue) {
    SettingAttribute ceConnector = SettingAttribute.Builder.aSettingAttribute()
                                       .withName(name)
                                       .withUuid(uuid)
                                       .withValue(settingValue)
                                       .withAccountId(accountId)
                                       .withCategory(SettingCategory.CE_CONNECTOR)
                                       .build();
    hPersistence.save(ceConnector);
  }

  public void createClusterRecord(
      String accountId, String clusterName, String clusterId, String cloudProviderId, String region) {
    Cluster cluster =
        EcsCluster.builder().clusterName(clusterName).cloudProviderId(cloudProviderId).region(region).build();
    clusterRecordService.upsert(
        ClusterRecord.builder().cluster(cluster).accountId(accountId).uuid(clusterId).createdAt(1L).build());
  }

  public void createCECluster(CECluster ceCluster) {
    clusterDao.create(ceCluster);
  }

  public void createCECloudAccount(CECloudAccount ceCloudAccount) {
    cloudAccountDao.create(ceCloudAccount);
  }
}
