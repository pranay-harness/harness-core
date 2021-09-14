/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.spotinst;

import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupInstanceHealth;

import java.util.List;
import java.util.Optional;

public interface SpotInstHelperServiceDelegate {
  List<ElastiGroup> listAllElstiGroups(String spotInstToken, String spotInstAccountId) throws Exception;
  List<ElastiGroup> listAllElastiGroups(String spotInstToken, String spotInstAccountId, String elastiGroupNamePrefix)
      throws Exception;
  String getElastigroupJson(String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception;
  Optional<ElastiGroup> getElastiGroupByName(String spotInstToken, String spotInstAccountId, String elastiGroupName)
      throws Exception;
  Optional<ElastiGroup> getElastiGroupById(String spotInstToken, String spotInstAccountId, String elastiGroupId)
      throws Exception;
  ElastiGroup createElastiGroup(String spotInstToken, String spotInstAccountId, String jsonPayload) throws Exception;
  void updateElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, String jsonPayload)
      throws Exception;
  void updateElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, Object group)
      throws Exception;
  void updateElastiGroupCapacity(
      String spotInstToken, String spotInstAccountId, String elastiGroupId, ElastiGroup group) throws Exception;
  void deleteElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception;
  void scaleUpElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception;
  void scaleDownElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception;
  List<ElastiGroupInstanceHealth> listElastiGroupInstancesHealth(
      String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception;
}
