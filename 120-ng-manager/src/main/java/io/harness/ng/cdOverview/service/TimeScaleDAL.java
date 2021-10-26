package io.harness.ng.cdOverview.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.timescaledb.Tables.ENVIRONMENTS;
import static io.harness.timescaledb.Tables.NG_INSTANCE_STATS;
import static io.harness.timescaledb.Tables.PIPELINE_EXECUTION_SUMMARY_CD;
import static io.harness.timescaledb.Tables.SERVICES;
import static io.harness.timescaledb.Tables.SERVICE_INFRA_INFO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dashboards.GroupBy;
import io.harness.ng.cdOverview.dto.AggregateProjectInfo;
import io.harness.ng.cdOverview.dto.AggregateServiceInfo;
import io.harness.ng.cdOverview.dto.TimeWiseExecutionSummary;
import io.harness.timescaledb.tables.pojos.PipelineExecutionSummaryCd;
import io.harness.timescaledb.tables.pojos.Services;

import com.google.inject.Inject;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Table;
import org.jooq.impl.DSL;

@OwnedBy(PIPELINE)
public class TimeScaleDAL {
  public static final int RECORDS_LIMIT = 100;
  public static final String ORG_ID = "orgId";
  public static final String PROJECT_ID = "projectId";
  public static final String SERVICE_ID = "serviceId";

  @Inject private DSLContext dsl;

  public List<AggregateServiceInfo> getTopServicesByDeploymentCount(@NotNull String accountIdentifier,
      long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    return dsl
        .select(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID,
            DSL.count().as("count"))
        .from(SERVICE_INFRA_INFO)
        .where(SERVICE_INFRA_INFO.ACCOUNTID.eq(accountIdentifier)
                   .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.greaterOrEqual(startInterval))
                   .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.lessThan(endInterval)))
        .andExists(dsl.selectOne()
                       .from(orgProjectTable)
                       .where(SERVICE_INFRA_INFO.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                                  .and(SERVICE_INFRA_INFO.PROJECTIDENTIFIER.eq(
                                      (Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .groupBy(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID)
        .orderBy(DSL.inline(4).desc())
        .limit(RECORDS_LIMIT)
        .fetchInto(AggregateServiceInfo.class);
  }

  public List<AggregateServiceInfo> getTopServicesByInstanceCount(
      String accountIdentifier, long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    Field<Long> reportedDateEpoch = DSL.epoch(NG_INSTANCE_STATS.REPORTEDAT).cast(Long.class).mul(1000);
    return dsl
        .select(
            NG_INSTANCE_STATS.ORGID, NG_INSTANCE_STATS.PROJECTID, NG_INSTANCE_STATS.SERVICEID, DSL.count().as("count"))
        .from(NG_INSTANCE_STATS)
        .where(NG_INSTANCE_STATS.ACCOUNTID.eq(accountIdentifier)
                   .and(reportedDateEpoch.greaterOrEqual(startInterval))
                   .and(reportedDateEpoch.lessThan(endInterval)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(NG_INSTANCE_STATS.ORGID.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(NG_INSTANCE_STATS.PROJECTID.eq((Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .groupBy(NG_INSTANCE_STATS.ORGID, NG_INSTANCE_STATS.PROJECTID, NG_INSTANCE_STATS.SERVICEID)
        .orderBy(DSL.inline(4).desc())
        .limit(RECORDS_LIMIT)
        .fetchInto(AggregateServiceInfo.class);
  }

  public List<AggregateServiceInfo> getInstanceCountForGivenServices(
      Table<Record3<String, String, String>> orgProjectServiceTable, String accountIdentifier, long startInterval,
      long endInterval) {
    Field<Long> reportedDateEpoch = DSL.epoch(NG_INSTANCE_STATS.REPORTEDAT).cast(Long.class).mul(1000);

    return dsl
        .select(
            NG_INSTANCE_STATS.ORGID, NG_INSTANCE_STATS.PROJECTID, NG_INSTANCE_STATS.SERVICEID, DSL.count().as("count"))
        .from(NG_INSTANCE_STATS)
        .where(NG_INSTANCE_STATS.ACCOUNTID.eq(accountIdentifier)
                   .and(reportedDateEpoch.greaterOrEqual(startInterval))
                   .and(reportedDateEpoch.lessThan(endInterval)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectServiceTable)
                .where(
                    NG_INSTANCE_STATS.ORGID.eq((Field<String>) orgProjectServiceTable.field(ORG_ID))
                        .and(NG_INSTANCE_STATS.PROJECTID.eq((Field<String>) orgProjectServiceTable.field(PROJECT_ID)))
                        .and(NG_INSTANCE_STATS.SERVICEID.eq((Field<String>) orgProjectServiceTable.field(SERVICE_ID)))))
        .groupBy(NG_INSTANCE_STATS.ORGID, NG_INSTANCE_STATS.PROJECTID, NG_INSTANCE_STATS.SERVICEID)
        .orderBy(DSL.inline(4).desc())
        .limit(RECORDS_LIMIT)
        .fetchInto(AggregateServiceInfo.class);
  }

  public List<Services> getNamesForServiceIds(
      String accountIdentifier, Table<Record3<String, String, String>> orgProjectServiceTable) {
    return dsl.select(SERVICES.ORG_IDENTIFIER, SERVICES.PROJECT_IDENTIFIER, SERVICES.IDENTIFIER, SERVICES.NAME)
        .from(SERVICES)
        .where(SERVICES.ACCOUNT_ID.eq(accountIdentifier))
        .andExists(
            dsl.selectOne()
                .from(orgProjectServiceTable)
                .where(
                    SERVICES.ORG_IDENTIFIER.eq((Field<String>) orgProjectServiceTable.field(ORG_ID))
                        .and(SERVICES.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectServiceTable.field(PROJECT_ID)))
                        .and(SERVICES.IDENTIFIER.eq((Field<String>) orgProjectServiceTable.field(SERVICE_ID)))))
        .fetchInto(Services.class);
  }

  public List<AggregateServiceInfo> getStatusWiseDeploymentCountForGivenServices(
      Table<Record3<String, String, String>> orgProjectServiceTable, String accountIdentifier, long startInterval,
      long endInterval) {
    return dsl
        .select(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID,
            SERVICE_INFRA_INFO.SERVICE_STATUS, DSL.count().as("count"))
        .from(SERVICE_INFRA_INFO)
        .where(SERVICE_INFRA_INFO.ACCOUNTID.eq(accountIdentifier)
                   .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.greaterOrEqual(startInterval))
                   .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.lessThan(endInterval)))
        .andExists(dsl.selectOne()
                       .from(orgProjectServiceTable)
                       .where(SERVICE_INFRA_INFO.ORGIDENTIFIER.eq((Field<String>) orgProjectServiceTable.field(ORG_ID))
                                  .and(SERVICE_INFRA_INFO.PROJECTIDENTIFIER.eq(
                                      (Field<String>) orgProjectServiceTable.field(PROJECT_ID)))
                                  .and(SERVICE_INFRA_INFO.SERVICE_ID.eq(
                                      (Field<String>) orgProjectServiceTable.field(SERVICE_ID)))))
        .groupBy(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID,
            SERVICE_INFRA_INFO.SERVICE_STATUS)
        .limit(RECORDS_LIMIT)
        .fetchInto(AggregateServiceInfo.class);
  }

  public List<AggregateServiceInfo> getDeploymentCountForGivenServices(
      Table<Record3<String, String, String>> orgProjectServiceTable, String accountIdentifier, long startInterval,
      long endInterval) {
    return dsl
        .select(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID,
            DSL.count().as("count"))
        .from(SERVICE_INFRA_INFO)
        .where(SERVICE_INFRA_INFO.ACCOUNTID.eq(accountIdentifier)
                   .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.greaterOrEqual(startInterval))
                   .and(SERVICE_INFRA_INFO.SERVICE_STARTTS.lessThan(endInterval)))
        .andExists(dsl.selectOne()
                       .from(orgProjectServiceTable)
                       .where(SERVICE_INFRA_INFO.ORGIDENTIFIER.eq((Field<String>) orgProjectServiceTable.field(ORG_ID))
                                  .and(SERVICE_INFRA_INFO.PROJECTIDENTIFIER.eq(
                                      (Field<String>) orgProjectServiceTable.field(PROJECT_ID)))
                                  .and(SERVICE_INFRA_INFO.SERVICE_ID.eq(
                                      (Field<String>) orgProjectServiceTable.field(SERVICE_ID)))))
        .groupBy(SERVICE_INFRA_INFO.ORGIDENTIFIER, SERVICE_INFRA_INFO.PROJECTIDENTIFIER, SERVICE_INFRA_INFO.SERVICE_ID)
        .limit(RECORDS_LIMIT)
        .fetchInto(AggregateServiceInfo.class);
  }

  List<AggregateProjectInfo> getTopProjectsByDeploymentCount(
      String accountIdentifier, long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    return dsl
        .select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
            DSL.count().as("count"))
        .from(PIPELINE_EXECUTION_SUMMARY_CD)
        .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startInterval))
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endInterval)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                               (Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .groupBy(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER)
        .orderBy(DSL.inline(3).desc())
        .limit(RECORDS_LIMIT)
        .fetchInto(AggregateProjectInfo.class);
  }

  public List<PipelineExecutionSummaryCd> getPipelineExecutionsForGivenExecutionStatus(
      String accountIdentifier, Table<Record2<String, String>> orgProjectTable, List<String> requiredStatuses) {
    return dsl
        .select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
            PIPELINE_EXECUTION_SUMMARY_CD.PIPELINEIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.NAME,
            PIPELINE_EXECUTION_SUMMARY_CD.STARTTS, PIPELINE_EXECUTION_SUMMARY_CD.STATUS,
            PIPELINE_EXECUTION_SUMMARY_CD.PLANEXECUTIONID)
        .from(PIPELINE_EXECUTION_SUMMARY_CD)
        .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STATUS.in(requiredStatuses)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                               (Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(PipelineExecutionSummaryCd.class);
  }

  List<TimeWiseExecutionSummary> getTimeExecutionStatusWiseDeploymentCount(String accountIdentifier, long startInterval,
      long endInterval, GroupBy groupBy, Table<Record2<String, String>> orgProjectTable) {
    Field<Long> epoch = DSL.field("time_bucket_gapfill(" + groupBy.getNoOfMilliseconds() + ", {0})", Long.class,
        PIPELINE_EXECUTION_SUMMARY_CD.STARTTS);

    return dsl.select(epoch, PIPELINE_EXECUTION_SUMMARY_CD.STATUS, DSL.count().as("count"))
        .from(PIPELINE_EXECUTION_SUMMARY_CD)
        .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startInterval))
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endInterval)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                               (Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .groupBy(DSL.one(), PIPELINE_EXECUTION_SUMMARY_CD.STATUS)
        .orderBy(DSL.one())
        .fetchInto(TimeWiseExecutionSummary.class);
  }

  Integer getNewServicesCount(
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
                .where(SERVICES.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(SERVICES.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(Integer.class)
        .get(0);
  }

  // Environments Created Previously but deleted in given time range
  Integer getDeletedEnvCount(
      String accountIdentifier, long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(ENVIRONMENTS)
        .where(ENVIRONMENTS.ACCOUNT_ID.eq(accountIdentifier))
        .and(ENVIRONMENTS.LAST_MODIFIED_AT.greaterOrEqual(startInterval))
        .and(ENVIRONMENTS.LAST_MODIFIED_AT.lessThan(endInterval))
        .and(ENVIRONMENTS.CREATED_AT.lessThan(startInterval))
        .and(ENVIRONMENTS.DELETED.eq(true))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(ENVIRONMENTS.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(ENVIRONMENTS.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(Integer.class)
        .get(0);
  }

  Integer getTotalServicesCount(String accountIdentifier, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(SERVICES)
        .where(SERVICES.ACCOUNT_ID.eq(accountIdentifier))
        .and(SERVICES.DELETED.eq(false))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(SERVICES.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(SERVICES.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(Integer.class)
        .get(0);
  }

  Integer getDeletedServiceCount(
      String accountIdentifier, long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(SERVICES)
        .where(SERVICES.ACCOUNT_ID.eq(accountIdentifier))
        .and(SERVICES.LAST_MODIFIED_AT.greaterOrEqual(startInterval))
        .and(SERVICES.LAST_MODIFIED_AT.lessThan(endInterval))
        .and(SERVICES.DELETED.eq(true))
        .and(SERVICES.CREATED_AT.lessThan(startInterval))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(SERVICES.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(SERVICES.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(Integer.class)
        .get(0);
  }

  Integer getTotalEnvCount(String accountIdentifier, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(ENVIRONMENTS)
        .where(ENVIRONMENTS.ACCOUNT_ID.eq(accountIdentifier))
        .and(ENVIRONMENTS.DELETED.eq(false))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(ENVIRONMENTS.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(ENVIRONMENTS.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(Integer.class)
        .get(0);
  }

  Integer getNewEnvCount(
      String accountIdentifier, long startInterval, long endInterval, Table<Record2<String, String>> orgProjectTable) {
    return dsl.select(DSL.count())
        .from(ENVIRONMENTS)
        .where(ENVIRONMENTS.ACCOUNT_ID.eq(accountIdentifier))
        .and(ENVIRONMENTS.CREATED_AT.greaterOrEqual(startInterval))
        .and(ENVIRONMENTS.CREATED_AT.lessThan(endInterval))
        .and(ENVIRONMENTS.DELETED.eq(false))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(ENVIRONMENTS.ORG_IDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(ENVIRONMENTS.PROJECT_IDENTIFIER.eq((Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(Integer.class)
        .get(0);
  }

  public List<PipelineExecutionSummaryCd> getFailedExecutionsForGivenTimeRange(
      String accountIdentifier, Table<Record2<String, String>> orgProjectTable, long endTime, long startTime) {
    return dsl
        .select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
            PIPELINE_EXECUTION_SUMMARY_CD.PIPELINEIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.NAME,
            PIPELINE_EXECUTION_SUMMARY_CD.STARTTS, PIPELINE_EXECUTION_SUMMARY_CD.STATUS,
            PIPELINE_EXECUTION_SUMMARY_CD.PLANEXECUTIONID)
        .from(PIPELINE_EXECUTION_SUMMARY_CD)
        .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STATUS.in(CDDashboardServiceHelper.failedStatusList))
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startTime))
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endTime)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                               (Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .fetchInto(PipelineExecutionSummaryCd.class);
  }

  public List<AggregateProjectInfo> getProjectWiseStatusWiseDeploymentCount(
      Table<Record2<String, String>> orgProjectTable, String accountIdentifier, long startInterval, long endInterval) {
    return dsl
        .select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
            PIPELINE_EXECUTION_SUMMARY_CD.STATUS, DSL.count().as("count"))
        .from(PIPELINE_EXECUTION_SUMMARY_CD)
        .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startInterval))
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endInterval)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                               (Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .groupBy(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
            PIPELINE_EXECUTION_SUMMARY_CD.STATUS)
        .fetchInto(AggregateProjectInfo.class);
  }

  public List<AggregateProjectInfo> getProjectWiseDeploymentCount(
      Table<Record2<String, String>> orgProjectTable, String accountIdentifier, long startInterval, long endInterval) {
    return dsl
        .select(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER,
            DSL.count().as("count"))
        .from(PIPELINE_EXECUTION_SUMMARY_CD)
        .where(PIPELINE_EXECUTION_SUMMARY_CD.ACCOUNTID.eq(accountIdentifier)
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.greaterOrEqual(startInterval))
                   .and(PIPELINE_EXECUTION_SUMMARY_CD.STARTTS.lessThan(endInterval)))
        .andExists(
            dsl.selectOne()
                .from(orgProjectTable)
                .where(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER.eq((Field<String>) orgProjectTable.field(ORG_ID))
                           .and(PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER.eq(
                               (Field<String>) orgProjectTable.field(PROJECT_ID)))))
        .groupBy(PIPELINE_EXECUTION_SUMMARY_CD.ORGIDENTIFIER, PIPELINE_EXECUTION_SUMMARY_CD.PROJECTIDENTIFIER)
        .fetchInto(AggregateProjectInfo.class);
  }
}
