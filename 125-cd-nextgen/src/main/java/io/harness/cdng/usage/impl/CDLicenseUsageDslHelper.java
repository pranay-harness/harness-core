package io.harness.cdng.usage.impl;

import static io.harness.timescaledb.Tables.NG_INSTANCE_STATS;

import static java.util.Objects.isNull;

import io.harness.cdng.usage.impl.AggregateServiceUsageInfo.AggregateServiceUsageInfoKeys;

import com.google.inject.Inject;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;

public class CDLicenseUsageDslHelper {
  @Inject private DSLContext dsl;

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

  long getAggregatedPercentileInstanceCount(
      String accountIdentifier, double percentile, long startInterval, long endInterval) {
    Field<Long> reportedDateEpoch = DSL.epoch(NG_INSTANCE_STATS.REPORTEDAT).cast(Long.class).mul(1000);

    Record instanceCountRecord =
        dsl.select(DSL.percentileDisc(percentile).withinGroupOrderBy(NG_INSTANCE_STATS.INSTANCECOUNT).as("count"))
            .from(NG_INSTANCE_STATS)
            .where(NG_INSTANCE_STATS.ACCOUNTID.eq(accountIdentifier)
                       .and(reportedDateEpoch.greaterOrEqual(startInterval))
                       .and(reportedDateEpoch.lessThan(endInterval)))
            .fetchOne();

    if (isNull(instanceCountRecord)) {
      return 0L;
    }
    return instanceCountRecord.getValue("count", long.class);
  }
}
