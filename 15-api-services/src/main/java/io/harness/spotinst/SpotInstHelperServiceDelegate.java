package io.harness.spotinst;

import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupInstanceHealth;

import java.util.List;
import java.util.Optional;

public interface SpotInstHelperServiceDelegate {
  List<ElastiGroup> listAllElastiGroups(String spotInstToken, String spotInstAccountId, String elastiGroupNamePrefix)
      throws Exception;
  Optional<ElastiGroup> getElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupName)
      throws Exception;
  ElastiGroup createElastiGroup(String spotInstToken, String spotInstAccountId, String jsonPayload) throws Exception;
  void updateElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, String jsonPayload)
      throws Exception;
  void deleteElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception;
  void scaleUpElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception;
  void scaleDownElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception;
  List<ElastiGroupInstanceHealth> listElastiGroupInstancesHealth(
      String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception;
}