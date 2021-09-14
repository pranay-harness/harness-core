/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;

import software.wings.audit.EntityAuditRecord;
import software.wings.beans.EntityType;
import software.wings.beans.ResourceLookup;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ResourceLookupService {
  /**
   * Creates the.
   *
   * @param header the header
   * @return the audit header
   */
  ResourceLookup create(ResourceLookup header);

  /**
   * List.
   *
   * @param req the req
   * @return the page response
   */
  PageResponse<ResourceLookup> list(PageRequest<ResourceLookup> req);

  void updateResourceName(ResourceLookup resourceLookup);

  void delete(ResourceLookup resourceLookup);

  List<String> listApplicationLevelResourceTypes();

  List<String> listAccountLevelResourceTypes();

  void deleteResourceLookupRecordIfNeeded(EntityAuditRecord record, String accountId);

  <T> void updateResourceLookupRecordIfNeeded(EntityAuditRecord record, String accountId, T newEntity, T oldEntity);

  void saveResourceLookupRecordIfNeeded(EntityAuditRecord record, String accountId);

  ResourceLookup getWithResourceId(String accountId, String resourceId);

  void updateResourceLookupRecordWithTags(
      String accountId, String entityId, String tagKey, String tagValue, boolean addTag);

  PageResponse<ResourceLookup> listResourceLookupRecordsWithTags(
      String accountId, String filter, String limit, String offset);

  <T> PageResponse<T> listWithTagFilters(
      PageRequest<T> request, String filter, EntityType entityType, boolean withTags);

  Map<String, ResourceLookup> getResourceLookupMapWithResourceIds(String accountId, Set<String> resourceIds);
}
