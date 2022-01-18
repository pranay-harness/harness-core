/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdlicense.impl;

import static io.harness.cdlicense.bean.CgCdLicenseUsageConstants.MAX_RETRY;
import static io.harness.cdlicense.bean.CgCdLicenseUsageConstants.QUERY_FECTH_PERCENTILE_INSTANCE_COUNT_FOR_SERVICES;
import static io.harness.cdlicense.bean.CgCdLicenseUsageConstants.QUERY_FECTH_SERVICES_IN_LAST_N_DAYS_DEPLOYMENT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.cdlicense.bean.CgServiceUsage;
import io.harness.cdlicense.exception.CgLicenseUsageException;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CgCdLicenseUsageQueryHelper {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject private WingsPersistence wingsPersistence;

  public List<String> fetchDistinctSvcIdUsedInDeployments(String accountId, int timePeriod) {
    int retry = 0;
    boolean successfulOperation = false;
    List<String> serviceIds = new ArrayList<>();

    while (!successfulOperation && retry < MAX_RETRY) {
      try (Connection dbConnection = timeScaleDBService.getDBConnection();
           PreparedStatement fetchStatement =
               dbConnection.prepareStatement(QUERY_FECTH_SERVICES_IN_LAST_N_DAYS_DEPLOYMENT)) {
        fetchStatement.setString(1, accountId);
        fetchStatement.setInt(2, timePeriod);

        ResultSet resultSet = fetchStatement.executeQuery();
        serviceIds = processResultSetToFetchServiceIds(resultSet);
        successfulOperation = true;
      } catch (SQLException exception) {
        if (retry >= MAX_RETRY) {
          String errorLog = "MAX RETRY FAILURE : Failed to fetch serviceIds within interval";
          throw new CgLicenseUsageException(errorLog, exception);
        }
        log.error(
            "Failed to fetch serviceIds within interval for last [{} days] deployments for accountId : [{}] , retry : [{}]",
            timePeriod, accountId, retry, exception);
        retry++;
      }
    }

    return serviceIds;
  }

  private List<String> processResultSetToFetchServiceIds(ResultSet resultSet) throws SQLException {
    List<String> serviceIds = new ArrayList<>();
    while (resultSet.next()) {
      serviceIds.add(resultSet.getString(1));
    }
    return serviceIds;
  }

  public Map<String, CgServiceUsage> getPercentileInstanceForServices(
      String accountId, List<String> svcIds, int timePeriod, double percentile) {
    if (isEmpty(svcIds)) {
      return Collections.emptyMap();
    }

    int retry = 0;
    boolean successfulOperation = false;
    Map<String, CgServiceUsage> serviceUsageMap = new HashMap<>();

    while (!successfulOperation && retry < MAX_RETRY) {
      try (Connection dbConnection = timeScaleDBService.getDBConnection();
           PreparedStatement fetchStatement =
               dbConnection.prepareStatement(QUERY_FECTH_PERCENTILE_INSTANCE_COUNT_FOR_SERVICES)) {
        fetchStatement.setDouble(1, percentile);
        fetchStatement.setString(2, accountId);
        fetchStatement.setArray(3, dbConnection.createArrayOf("text", svcIds.toArray()));
        fetchStatement.setInt(4, timePeriod);

        ResultSet resultSet = fetchStatement.executeQuery();
        serviceUsageMap = processResultSetToCreateServiceUsageMap(resultSet);
        successfulOperation = true;
      } catch (SQLException exception) {
        if (retry >= MAX_RETRY) {
          String errorLog = "MAX RETRY FAILURE : Failed to fetch service usage within interval";
          throw new CgLicenseUsageException(errorLog, exception);
        }
        log.error("Failed to fetch service usage for accountId : [{}] , retry : [{}]", accountId, retry, exception);
        retry++;
      }
    }

    return serviceUsageMap;
  }

  private Map<String, CgServiceUsage> processResultSetToCreateServiceUsageMap(ResultSet resultSet) throws SQLException {
    Map<String, CgServiceUsage> serviceUsageMap = new HashMap<>();
    while (resultSet.next()) {
      serviceUsageMap.put(resultSet.getString(1),
          CgServiceUsage.builder().serviceId(resultSet.getString(1)).instanceCount(resultSet.getInt(2)).build());
    }

    return serviceUsageMap;
  }

  public Map<String, String> fetchServicesNames(String accountId, List<String> serviceUuids) {
    if (isEmpty(serviceUuids)) {
      return Collections.emptyMap();
    }
    serviceUuids = serviceUuids.stream().distinct().collect(toList());
    List<Service> services = wingsPersistence.createQuery(Service.class)
                                 .filter(Service.ServiceKeys.accountId, accountId)
                                 .field(Service.ServiceKeys.uuid)
                                 .in(serviceUuids)
                                 .project(Service.ServiceKeys.uuid, true)
                                 .project(Service.ServiceKeys.name, true)
                                 .asList();
    return services.stream().collect(Collectors.toMap(Service::getUuid, Service::getName));
  }
}
