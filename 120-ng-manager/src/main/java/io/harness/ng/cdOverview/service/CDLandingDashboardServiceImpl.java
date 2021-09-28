package io.harness.ng.cdOverview.service;

import static io.harness.timescaledb.Tables.*;

import static org.jooq.impl.DSL.row;

import io.harness.dashboards.*;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.OrgProjectIdentifier;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.timescaledb.tables.pojos.PipelineExecutionSummaryCd;
import io.harness.timescaledb.tables.pojos.ServiceInfraInfo;
import io.harness.timescaledb.tables.pojos.Services;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Row2;
import org.jooq.Row3;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class CDLandingDashboardServiceImpl implements CDLandingDashboardService {
  public static final int RECORDS_LIMIT = 100;

  @Inject private DSLContext dsl;
  @Inject private ServiceEntityService serviceEntityService;

  @Getter
  @EqualsAndHashCode(callSuper = true)
  public static class AggregateServiceInfo extends ServiceInfraInfo {
    private long count;

    public AggregateServiceInfo(ServiceInfraInfo value, long count) {
      super(value);
      this.count = count;
    }

    public AggregateServiceInfo(String id, String serviceName, String serviceId, String tag, String envName,
        String envId, String envType, String pipelineExecutionSummaryCdId, String deploymentType, String serviceStatus,
        Long serviceStartts, Long serviceEndts, String orgidentifier, String accountid, String projectidentifier,
        String artifactImage, long count) {
      super(id, serviceName, serviceId, tag, envName, envId, envType, pipelineExecutionSummaryCdId, deploymentType,
          serviceStatus, serviceStartts, serviceEndts, accountid, orgidentifier, projectidentifier, artifactImage);
      this.count = count;
    }

    public AggregateServiceInfo(String orgIdentifier, String projectId, String serviceId, long count) {
      super(null, null, serviceId, null, null, null, null, null, null, null, null, null, null, orgIdentifier, projectId,
          null);
      this.count = count;
    }

    public AggregateServiceInfo(
        String orgIdentifier, String projectId, String serviceId, String serviceStatus, long count) {
      super(null, null, serviceId, null, null, null, null, null, null, serviceStatus, null, null, null, orgIdentifier,
          projectId, null);
      this.count = count;
    }
  }

  @Override
  public ServicesDashboardInfo getActiveServices(@NotNull String accountIdentifier,
      @NotNull List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval,
      @NotNull SortBy sortBy) {
    Table<Record2<String, String>> orgProjectTable = getOrgProjectTable(orgProjectIdentifiers);

    List<AggregateServiceInfo> serviceInfraInfoList =
        dsl.select(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER,
               SERVICE_INFRA_INFO.SERVICE_ID, DSL.count().as("count"))
            .from(SERVICE_INFRA_INFO)
            .where(SERVICE_INFRA_INFO.ACCOUNTID.eq(accountIdentifier)
                       .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.greaterOrEqual(startInterval))
                       .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.lessThan(endInterval)))
            .andExists(dsl.selectOne()
                           .from(orgProjectTable)
                           .where(SERVICE_INFRA_INFO.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field("orgId"))
                                      .and(SERVICE_INFRA_INFO.PROJECTIDENTIFIER.eq(
                                          (Field<String>) orgProjectTable.field("projectId")))))
            .groupBy(
                SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID)
            .orderBy(DSL.inline(4).desc())
            .limit(RECORDS_LIMIT)
            .fetchInto(AggregateServiceInfo.class);

    List<ServiceDashboardInfo> servicesDashboardInfoList = new ArrayList<>();

    if (EmptyPredicate.isEmpty(serviceInfraInfoList)) {
      return ServicesDashboardInfo.builder().build();
    }

    for (AggregateServiceInfo serviceInfraInfo : serviceInfraInfoList) {
      servicesDashboardInfoList.add(ServiceDashboardInfo.builder()
                                        .identifier(serviceInfraInfo.getServiceId())
                                        .accountIdentifier(accountIdentifier)
                                        .orgIdentifier(serviceInfraInfo.getOrgidentifier())
                                        .projectIdentifier(serviceInfraInfo.getProjectidentifier())
                                        .totalDeploymentsCount(serviceInfraInfo.getCount())
                                        .build());
    }

    Table<Record3<String, String, String>> orgProjectServiceTable = getOrgProjectServiceTable(serviceInfraInfoList);

    prepareChangeRate(
        orgProjectServiceTable, accountIdentifier, startInterval, endInterval, sortBy, servicesDashboardInfoList);

    prepareStatusWiseCount(
        orgProjectServiceTable, accountIdentifier, startInterval, endInterval, sortBy, servicesDashboardInfoList);

    addServiceNames(servicesDashboardInfoList, accountIdentifier, orgProjectServiceTable);

    return ServicesDashboardInfo.builder().serviceDashboardInfoList(servicesDashboardInfoList).build();
  }

  private void addServiceNames(List<ServiceDashboardInfo> serviceDashboardInfoList, String accountIdentifier,
      Table<Record3<String, String, String>> orgProjectServiceTable) {
    List<Services> servicesList =
        dsl.select(SERVICES.ORG_IDENTIFIER, SERVICES.PROJECT_IDENTIFIER, SERVICES.IDENTIFIER, SERVICES.NAME)
            .from(SERVICES)
            .where(SERVICES.ACCOUNT_ID.eq(accountIdentifier))
            .andExists(
                dsl.selectOne()
                    .from(orgProjectServiceTable)
                    .where(SERVICES.ORG_IDENTIFIER.eq((Field<String>) orgProjectServiceTable.field("orgId"))
                               .and(SERVICES.PROJECT_IDENTIFIER.eq(
                                   (Field<String>) orgProjectServiceTable.field("projectId")))
                               .and(SERVICES.IDENTIFIER.eq((Field<String>) orgProjectServiceTable.field("serviceId")))))
            .fetchInto(Services.class);

    Map<String, ServiceDashboardInfo> combinedIdToRecordMap = new HashMap<>();

    for (ServiceDashboardInfo serviceDashboardInfo : serviceDashboardInfoList) {
      String key = getCombinedId(serviceDashboardInfo.getOrgIdentifier(), serviceDashboardInfo.getProjectIdentifier(),
          serviceDashboardInfo.getIdentifier());
      combinedIdToRecordMap.put(key, serviceDashboardInfo);
    }

    for (Services service : servicesList) {
      String key = getCombinedId(service.getOrgIdentifier(), service.getProjectIdentifier(), service.getIdentifier());
      ServiceDashboardInfo serviceDashboardInfo = combinedIdToRecordMap.get(key);

      serviceDashboardInfo.setName(service.getName());
    }
  }

  private void prepareStatusWiseCount(Table<Record3<String, String, String>> orgProjectServiceTable,
      String accountIdentifier, long startInterval, long endInterval, SortBy sortBy,
      List<ServiceDashboardInfo> serviceDashboardInfoList) {
    if (EmptyPredicate.isEmpty(serviceDashboardInfoList)) {
      return;
    }

    List<AggregateServiceInfo> previousServiceInfraInfoList =
        dsl.select(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER,
               SERVICE_INFRA_INFO.SERVICE_ID, SERVICE_INFRA_INFO.SERVICE_STATUS, DSL.count().as("count"))
            .from(SERVICE_INFRA_INFO)
            .where(SERVICE_INFRA_INFO.ACCOUNTID.eq(accountIdentifier)
                       .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.greaterOrEqual(startInterval))
                       .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.lessThan(endInterval)))
            .andExists(
                dsl.selectOne()
                    .from(orgProjectServiceTable)
                    .where(SERVICE_INFRA_INFO.ORGIDENTIFIER.eq((Field<String>) orgProjectServiceTable.field("orgId"))
                               .and(SERVICE_INFRA_INFO.PROJECTIDENTIFIER.eq(
                                   (Field<String>) orgProjectServiceTable.field("projectId")))
                               .and(SERVICE_INFRA_INFO.SERVICE_ID.eq(
                                   (Field<String>) orgProjectServiceTable.field("serviceId")))))
            .groupBy(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER,
                SERVICE_INFRA_INFO.SERVICE_ID, SERVICE_INFRA_INFO.SERVICE_STATUS)
            .limit(RECORDS_LIMIT)
            .fetchInto(AggregateServiceInfo.class);

    Map<String, ServiceDashboardInfo> combinedIdToRecordMap = new HashMap<>();

    for (ServiceDashboardInfo serviceDashboardInfo : serviceDashboardInfoList) {
      String key = getCombinedId(serviceDashboardInfo.getOrgIdentifier(), serviceDashboardInfo.getProjectIdentifier(),
          serviceDashboardInfo.getIdentifier());
      combinedIdToRecordMap.put(key, serviceDashboardInfo);
    }

    for (AggregateServiceInfo aggregateServiceInfo : previousServiceInfraInfoList) {
      String key = getCombinedId(aggregateServiceInfo.getOrgidentifier(), aggregateServiceInfo.getProjectidentifier(),
          aggregateServiceInfo.getServiceId());
      ServiceDashboardInfo serviceDashboardInfo = combinedIdToRecordMap.get(key);

      String status = aggregateServiceInfo.getServiceStatus();
      if (ExecutionStatus.SUCCESS.name().equals(status)) {
        serviceDashboardInfo.setSuccessDeploymentsCount(aggregateServiceInfo.getCount());
      } else if (CDDashboardServiceHelper.failedStatusList.contains(status)) {
        serviceDashboardInfo.setFailureDeploymentsCount(
            aggregateServiceInfo.getCount() + serviceDashboardInfo.getFailureDeploymentsCount());
      }
    }
  }

  private void prepareChangeRate(Table<Record3<String, String, String>> orgProjectServiceTable,
      String accountIdentifier, long startInterval, long endInterval, SortBy sortBy,
      List<ServiceDashboardInfo> serviceDashboardInfoList) {
    long duration = endInterval - startInterval;
    startInterval -= duration;
    endInterval -= duration;

    List<AggregateServiceInfo> previousServiceInfraInfoList =
        dsl.select(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER,
               SERVICE_INFRA_INFO.SERVICE_ID, DSL.count().as("count"))
            .from(SERVICE_INFRA_INFO)
            .where(SERVICE_INFRA_INFO.ACCOUNTID.eq(accountIdentifier)
                       .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.greaterOrEqual(startInterval))
                       .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.lessThan(endInterval)))
            .andExists(
                dsl.selectOne()
                    .from(orgProjectServiceTable)
                    .where(SERVICE_INFRA_INFO.ORGIDENTIFIER.eq((Field<String>) orgProjectServiceTable.field("orgId"))
                               .and(SERVICE_INFRA_INFO.PROJECTIDENTIFIER.eq(
                                   (Field<String>) orgProjectServiceTable.field("projectId")))
                               .and(SERVICE_INFRA_INFO.SERVICE_ID.eq(
                                   (Field<String>) orgProjectServiceTable.field("serviceId")))))
            .groupBy(
                SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID)
            .limit(RECORDS_LIMIT)
            .fetchInto(AggregateServiceInfo.class);

    Map<String, AggregateServiceInfo> combinedIdToRecordMap = new HashMap<>();

    for (AggregateServiceInfo aggregateServiceInfo : previousServiceInfraInfoList) {
      String key = getCombinedId(aggregateServiceInfo.getOrgidentifier(), aggregateServiceInfo.getProjectidentifier(),
          aggregateServiceInfo.getServiceId());
      combinedIdToRecordMap.put(key, aggregateServiceInfo);
    }

    for (ServiceDashboardInfo serviceDashboardInfo : serviceDashboardInfoList) {
      String key = getCombinedId(serviceDashboardInfo.getOrgIdentifier(), serviceDashboardInfo.getProjectIdentifier(),
          serviceDashboardInfo.getIdentifier());
      if (combinedIdToRecordMap.containsKey(key)) {
        AggregateServiceInfo previousServiceInfo = combinedIdToRecordMap.get(key);
        long previousDeploymentsCount = previousServiceInfo.getCount();
        long change = serviceDashboardInfo.getTotalDeploymentsCount() - previousDeploymentsCount;

        if (previousDeploymentsCount != 0) {
          double changeRate = (change * 100.0) / previousDeploymentsCount;
          serviceDashboardInfo.setTotalDeploymentsChangeRate(changeRate);
        }
      }
    }
  }

  @org.jetbrains.annotations.NotNull
  private String getCombinedId(String... keys) {
    StringBuilder combinedId = new StringBuilder();

    for (String key : keys) {
      combinedId.append(key).append(" ");
    }
    combinedId.deleteCharAt(combinedId.length() - 1);

    return combinedId.toString();
  }

  private Table<Record3<String, String, String>> getOrgProjectServiceTable(
      List<AggregateServiceInfo> serviceInfraInfoList) {
    Row3<String, String, String>[] orgProjectServiceRows = new Row3[serviceInfraInfoList.size()];
    int index = 0;
    for (AggregateServiceInfo aggregateServiceInfo : serviceInfraInfoList) {
      orgProjectServiceRows[index++] = row(aggregateServiceInfo.getOrgidentifier(),
          aggregateServiceInfo.getProjectidentifier(), aggregateServiceInfo.getServiceId());
    }

    return DSL.values(orgProjectServiceRows).as("t", "orgId", "projectId", "serviceId");
  }

  @org.jetbrains.annotations.NotNull
  private Table<Record2<String, String>> getOrgProjectTable(@NotNull List<OrgProjectIdentifier> orgProjectIdentifiers) {
    Row2<String, String>[] orgProjectRows = new Row2[orgProjectIdentifiers.size()];
    int index = 0;
    for (OrgProjectIdentifier orgProjectIdentifier : orgProjectIdentifiers) {
      orgProjectRows[index++] =
          row(orgProjectIdentifier.getOrgIdentifier(), orgProjectIdentifier.getProjectIdentifier());
    }

    return DSL.values(orgProjectRows).as("t", "orgId", "projectId");
  }

  @Getter
  @EqualsAndHashCode(callSuper = true)
  public static class AggregateProjectInfo extends PipelineExecutionSummaryCd {
    private final long count;

    public AggregateProjectInfo(String orgIdentifier, String projectId, long count) {
      this.count = count;
      this.setOrgidentifier(orgIdentifier);
      this.setProjectidentifier(projectId);
    }

    public AggregateProjectInfo(String orgIdentifier, String projectId, String status, long count) {
      this.count = count;
      this.setOrgidentifier(orgIdentifier);
      this.setProjectidentifier(projectId);
      this.setStatus(status);
    }
  }

  @Override
  public ProjectsDashboardInfo getTopProjects(String accountIdentifier,
      List<OrgProjectIdentifier> orgProjectIdentifiers, long startInterval, long endInterval) {
    Table<Record2<String, String>> orgProjectTable = getOrgProjectTable(orgProjectIdentifiers);

    List<AggregateProjectInfo> projectInfoList =
        dsl.select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
               DSL.count().as("count"))
            .from(PIPELINE_EXECUTION_SUMMARY_CD)
            .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                       .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startInterval))
                       .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endInterval)))
            .andExists(dsl.selectOne()
                           .from(orgProjectTable)
                           .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER
                                      .eq((Field<String>) orgProjectTable.field("orgId"))
                                      .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                                          (Field<String>) orgProjectTable.field("projectId")))))
            .groupBy(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER)
            .orderBy(DSL.inline(3).desc())
            .limit(RECORDS_LIMIT)
            .fetchInto(AggregateProjectInfo.class);

    List<ProjectDashBoardInfo> projectDashBoardInfoList = new ArrayList<>();

    if (EmptyPredicate.isEmpty(projectInfoList)) {
      return ProjectsDashboardInfo.builder().build();
    }

    for (AggregateProjectInfo projectInfo : projectInfoList) {
      projectDashBoardInfoList.add(ProjectDashBoardInfo.builder()
                                       .accountId(accountIdentifier)
                                       .orgIdentifier(projectInfo.getOrgidentifier())
                                       .projectIdentifier(projectInfo.getProjectidentifier())
                                       .deploymentsCount(projectInfo.getCount())
                                       .build());
    }

    Table<Record2<String, String>> topOrgProjectTable = prepareOrgProjectTable(projectInfoList);
    Map<String, ProjectDashBoardInfo> combinedIdToRecordMap = getCombinedIdToRecordMap(projectDashBoardInfoList);

    prepareChangeRate(topOrgProjectTable, accountIdentifier, startInterval, endInterval, combinedIdToRecordMap);

    prepareStatusWiseCount(topOrgProjectTable, accountIdentifier, startInterval, endInterval, combinedIdToRecordMap);

    return ProjectsDashboardInfo.builder().projectDashBoardInfoList(projectDashBoardInfoList).build();
  }

  @Override
  public ServicesCount getServicesCount(String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers,
      long startInterval, long endInterval) {
    Table<Record2<String, String>> orgProjectTable = getOrgProjectTable(orgProjectIdentifiers);

    Integer totalServicesCount = getTotalServicesCount(accountIdentifier, orgProjectTable);
    Integer newCount = getServicesCount(accountIdentifier, startInterval, endInterval, orgProjectTable);

    return ServicesCount.builder().totalCount(totalServicesCount).newCount(newCount).build();
  }

  @Override
  public EnvCount getEnvCount(String accountIdentifier, List<OrgProjectIdentifier> orgProjectIdentifiers,
      long startInterval, long endInterval) {
    Table<Record2<String, String>> orgProjectTable = getOrgProjectTable(orgProjectIdentifiers);

    Integer totalCount = getTotalEnvCount(accountIdentifier, orgProjectTable);
    Integer newCount = getEnvCount(accountIdentifier, startInterval, endInterval, orgProjectTable);

    return EnvCount.builder().totalCount(totalCount).newCount(newCount).build();
  }

  private Integer getTotalEnvCount(String accountIdentifier, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(ENVIRONMENTS)
        .where(ENVIRONMENTS.ACCOUNT_ID.eq(accountIdentifier))
        .and(ENVIRONMENTS.DELETED.eq(false))
        .andExists(dsl.selectOne()
                       .from(orgProjectTable)
                       .where(ENVIRONMENTS.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field("orgId"))
                                  .and(ENVIRONMENTS.PROJECT_IDENTIFIER.eq(
                                      (Field<String>) orgProjectTable.field("projectId")))))
        .fetchInto(Integer.class)
        .get(0);
  }

  private Integer getEnvCount(
      String accountIdentifier, long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(ENVIRONMENTS)
        .where(ENVIRONMENTS.ACCOUNT_ID.eq(accountIdentifier))
        .and(ENVIRONMENTS.CREATED_AT.greaterOrEqual(startInterval))
        .and(ENVIRONMENTS.CREATED_AT.lessThan(endInterval))
        .and(ENVIRONMENTS.DELETED.eq(false))
        .andExists(dsl.selectOne()
                       .from(orgProjectTable)
                       .where(ENVIRONMENTS.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field("orgId"))
                                  .and(ENVIRONMENTS.PROJECT_IDENTIFIER.eq(
                                      (Field<String>) orgProjectTable.field("projectId")))))
        .fetchInto(Integer.class)
        .get(0);
  }

  private Integer getTotalServicesCount(String accountIdentifier, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(SERVICES)
        .where(SERVICES.ACCOUNT_ID.eq(accountIdentifier))
        .and(SERVICES.DELETED.eq(false))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(SERVICES.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field("orgId"))
                           .and(SERVICES.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field("projectId")))))
        .fetchInto(Integer.class)
        .get(0);
  }

  private Integer getServicesCount(
      String accountIdentifier, long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(SERVICES)
        .where(SERVICES.ACCOUNT_ID.eq(accountIdentifier))
        .and(SERVICES.CREATED_AT.greaterOrEqual(startInterval))
        .and(SERVICES.CREATED_AT.lessThan(endInterval))
        .and(SERVICES.DELETED.eq(false))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(SERVICES.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field("orgId"))
                           .and(SERVICES.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field("projectId")))))
        .fetchInto(Integer.class)
        .get(0);
  }

  private Map<String, ProjectDashBoardInfo> getCombinedIdToRecordMap(List<ProjectDashBoardInfo> projectInfoList) {
    Map<String, ProjectDashBoardInfo> combinedIdToRecordMap = new HashMap<>();

    for (ProjectDashBoardInfo projectDashBoardInfo : projectInfoList) {
      String key = getCombinedId(projectDashBoardInfo.getOrgIdentifier(), projectDashBoardInfo.getProjectIdentifier());
      combinedIdToRecordMap.put(key, projectDashBoardInfo);
    }

    return combinedIdToRecordMap;
  }

  private void prepareStatusWiseCount(Table<Record2<String, String>> orgProjectTable, String accountIdentifier,
      long startInterval, long endInterval, Map<String, ProjectDashBoardInfo> combinedIdToRecordMap) {
    List<AggregateProjectInfo> projectInfoList =
        dsl.select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
               PIPELINE_EXECUTION_SUMMARY_CD.STATUS, DSL.count().as("count"))
            .from(PIPELINE_EXECUTION_SUMMARY_CD)
            .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                       .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startInterval))
                       .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endInterval)))
            .andExists(dsl.selectOne()
                           .from(orgProjectTable)
                           .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER
                                      .eq((Field<String>) orgProjectTable.field("orgId"))
                                      .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                                          (Field<String>) orgProjectTable.field("projectId")))))
            .groupBy(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
                PIPELINE_EXECUTION_SUMMARY_CD.STATUS)
            .fetchInto(AggregateProjectInfo.class);

    for (AggregateProjectInfo aggregateProjectInfo : projectInfoList) {
      String key = getCombinedId(aggregateProjectInfo.getOrgidentifier(), aggregateProjectInfo.getProjectidentifier());
      ProjectDashBoardInfo projectDashBoardInfo = combinedIdToRecordMap.get(key);

      String status = aggregateProjectInfo.getStatus();
      if (ExecutionStatus.SUCCESS.name().equals(status)) {
        projectDashBoardInfo.setSuccessDeploymentsCount(aggregateProjectInfo.getCount());
      } else if (CDDashboardServiceHelper.failedStatusList.contains(status)) {
        projectDashBoardInfo.setFailedDeploymentsCount(
            aggregateProjectInfo.getCount() + projectDashBoardInfo.getFailedDeploymentsCount());
      }
    }
  }

  private void prepareChangeRate(Table<Record2<String, String>> orgProjectTable, String accountIdentifier,
      long startInterval, long endInterval, Map<String, ProjectDashBoardInfo> combinedIdToRecordMap) {
    long duration = endInterval - startInterval;
    startInterval -= duration;
    endInterval -= duration;

    List<AggregateProjectInfo> previousProjectInfoList =
        dsl.select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
               DSL.count().as("count"))
            .from(PIPELINE_EXECUTION_SUMMARY_CD)
            .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                       .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startInterval))
                       .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endInterval)))
            .andExists(dsl.selectOne()
                           .from(orgProjectTable)
                           .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER
                                      .eq((Field<String>) orgProjectTable.field("orgId"))
                                      .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                                          (Field<String>) orgProjectTable.field("projectId")))))
            .groupBy(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER)
            .fetchInto(AggregateProjectInfo.class);

    for (AggregateProjectInfo previousProjectInfo : previousProjectInfoList) {
      String key = getCombinedId(previousProjectInfo.getOrgidentifier(), previousProjectInfo.getProjectidentifier());
      ProjectDashBoardInfo projectDashBoardInfo = combinedIdToRecordMap.get(key);
      projectDashBoardInfo.setDeploymentsCountChangeRate(
          getChangeRate(previousProjectInfo.getCount(), projectDashBoardInfo.getDeploymentsCount()));
    }
  }

  private double getChangeRate(long previousValue, long newValue) {
    long change = newValue - previousValue;
    return previousValue != 0 ? (change * 100.0) / previousValue : 0;
  }

  private Table<Record2<String, String>> prepareOrgProjectTable(List<AggregateProjectInfo> projectInfoList) {
    List<OrgProjectIdentifier> orgProjectIdentifiers =
        projectInfoList.stream()
            .map(aggregateProjectInfo
                -> OrgProjectIdentifier.builder()
                       .orgIdentifier(aggregateProjectInfo.getOrgidentifier())
                       .projectIdentifier(aggregateProjectInfo.getProjectidentifier())
                       .build())
            .collect(Collectors.toList());

    return getOrgProjectTable(orgProjectIdentifiers);
  }
}
