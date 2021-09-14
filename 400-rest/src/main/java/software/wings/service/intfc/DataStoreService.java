/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.persistence.GoogleDataStoreAware;

import java.util.List;
import java.util.Map;

public interface DataStoreService {
  <T extends GoogleDataStoreAware> void save(Class<T> clazz, List<T> records, boolean ignoreDuplicate);

  <T extends GoogleDataStoreAware> int getNumberOfResults(Class<T> clazz, PageRequest<T> pageRequest);

  <T extends GoogleDataStoreAware> PageResponse<T> list(Class<T> clazz, PageRequest<T> pageRequest);
  <T extends GoogleDataStoreAware> PageResponse<T> list(
      Class<T> clazz, PageRequest<T> pageRequest, boolean getTotalRecords);
  <T extends GoogleDataStoreAware> T getEntity(Class<T> clazz, String id);
  <T extends GoogleDataStoreAware> void incrementField(Class<T> clazz, String id, String fieldName, int incrementCount);

  void delete(Class<? extends GoogleDataStoreAware> clazz, String id);

  void purgeByActivity(String appId, String activityId);

  void purgeOlderRecords();
  void purgeDataRetentionOlderRecords(Map<String, Long> accounts);

  boolean supportsInOperator();

  void delete(Class<? extends GoogleDataStoreAware> clazz, String fieldName, String fieldValue);
}
