/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.usage.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.timescaledb.Tables.NG_INSTANCE_STATS;
import static io.harness.timescaledb.Tables.SERVICES;

import static org.jooq.impl.DSL.row;

import io.harness.cdng.usage.impl.AggregateServiceUsageInfo.AggregateServiceUsageInfoKeys;
import io.harness.dtos.InstanceDTO;
import io.harness.timescaledb.tables.pojos.Services;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
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

  List<AggregateServiceUsageInfo> getActiveServicesInfoWithPercentileServiceInstanceCount(String accountIdentifier,
      double percentile, long startInterval, long endInterval,
      Table<Record3<String, String, String>> orgProjectServiceTable) {
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
        .andExists(
            dsl.selectOne()
                .from(orgProjectServiceTable)
                .where(
                    NG_INSTANCE_STATS.ORGID.eq((Field<String>) orgProjectServiceTable.field(ORG_ID))
                        .and(NG_INSTANCE_STATS.PROJECTID.eq((Field<String>) orgProjectServiceTable.field(PROJECT_ID)))
                        .and(NG_INSTANCE_STATS.SERVICEID.eq((Field<String>) orgProjectServiceTable.field(SERVICE_ID)))))
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

  public Table<Record3<String, String, String>> getOrgProjectServiceTableFromInstances(
      List<InstanceDTO> instanceDTOList) {
    if (isEmpty(instanceDTOList)) {
      return null;
    }
    Row3<String, String, String>[] orgProjectServiceRows = getOrgProjectServiceRows(instanceDTOList);

    return DSL.values(orgProjectServiceRows).as("t", "orgId", "projectId", "serviceId");
  }

  @VisibleForTesting
  @NotNull
  Row3<String, String, String>[] getOrgProjectServiceRows(List<InstanceDTO> instanceDTOList) {
    Map<String, UniqueServiceEntityId> uniqueServiceEntityIdMap =
        instanceDTOList.stream().collect(Collectors.toMap(this::getUniqueServiceOrgProjectId,
            instanceDTO1
            -> new UniqueServiceEntityId(instanceDTO1.getServiceIdentifier(), instanceDTO1.getProjectIdentifier(),
                instanceDTO1.getOrgIdentifier()),
            (entry1, entry2) -> entry1));

    Row3<String, String, String>[] orgProjectServiceRows = new Row3[uniqueServiceEntityIdMap.size()];

    int index = 0;
    for (UniqueServiceEntityId uniqueServiceEntityId : uniqueServiceEntityIdMap.values()) {
      orgProjectServiceRows[index++] = row(uniqueServiceEntityId.getOrgIdentifier(),
          uniqueServiceEntityId.getProjectIdentifier(), uniqueServiceEntityId.getServiceIdentifier());
    }
    return orgProjectServiceRows;
  }

  private String getUniqueServiceOrgProjectId(InstanceDTO instanceDTO) {
    return String.join(
        "&", instanceDTO.getOrgIdentifier(), instanceDTO.getProjectIdentifier(), instanceDTO.getServiceIdentifier());
  }

  private class UniqueServiceEntityId {
    @Getter private final String serviceIdentifier;
    @Getter private final String projectIdentifier;
    @Getter private final String orgIdentifier;

    private UniqueServiceEntityId(String serviceIdentifier, String projectIdentifier, String orgIdentifier) {
      this.serviceIdentifier = serviceIdentifier;
      this.projectIdentifier = projectIdentifier;
      this.orgIdentifier = orgIdentifier;
    }
  }
}
