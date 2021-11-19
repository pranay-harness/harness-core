package io.harness.cdng.usage.impl;

import static io.harness.timescaledb.Tables.NG_INSTANCE_STATS;
import static io.harness.timescaledb.Tables.SERVICES;

import static org.jooq.impl.DSL.row;

import io.harness.cdng.usage.impl.AggregateServiceUsageInfo.AggregateServiceUsageInfoKeys;
import io.harness.timescaledb.tables.pojos.Services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Singleton
public class CDLicenseUsageDslHelper {
  @Inject private DSLContext dsl;

  public static final String ORG_ID = "orgId";
  public static final String PROJECT_ID = "projectId";
  public static final String SERVICE_ID = "serviceId";

  List<AggregateServiceUsageInfo> getActiveServicesInfoWithPercentileServiceInstanceCount(
      String accountIdentifier, double percentile, long startInterval, long endInterval) {
    Field<Long> reportedDateEpoch = DSL.epoch(NG_INSTANCE_STATS.REPORTEDAT).cast(Long.class).mul(1000);
    return dsl
        .select(NG_INSTANCE_STATS.ORGID, NG_INSTANCE_STATS.PROJECTID, NG_INSTANCE_STATS.SERVICEID,
            DSL.percentileDisc(percentile)
                .withinGroupOrderBy(NG_INSTANCE_STATS.INSTANCECOUNT)
                .as(AggregateServiceUsageInfoKeys.activeInstanceCount))
        .from(NG_INSTANCE_STATS)
        .where(NG_INSTANCE_STATS.ACCOUNTID.eq(accountIdentifier)
                   .and(reportedDateEpoch.greaterOrEqual(startInterval))
                   .and(reportedDateEpoch.lessOrEqual(endInterval)))
        .groupBy(NG_INSTANCE_STATS.ORGID, NG_INSTANCE_STATS.PROJECTID, NG_INSTANCE_STATS.SERVICEID)
        .fetchInto(AggregateServiceUsageInfo.class);
  }

  public List<Services> getServiceEntities(
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

  public Table<Record3<String, String, String>> getOrgProjectServiceTable(
      List<AggregateServiceUsageInfo> serviceUsageInfoList) {
    Row3<String, String, String>[] orgProjectServiceRows = new Row3[serviceUsageInfoList.size()];
    int index = 0;
    for (AggregateServiceUsageInfo aggregateServiceInfo : serviceUsageInfoList) {
      orgProjectServiceRows[index++] = row(aggregateServiceInfo.getOrgidentifier(),
          aggregateServiceInfo.getProjectidentifier(), aggregateServiceInfo.getServiceId());
    }

    return DSL.values(orgProjectServiceRows).as("t", "orgId", "projectId", "serviceId");
  }
}
