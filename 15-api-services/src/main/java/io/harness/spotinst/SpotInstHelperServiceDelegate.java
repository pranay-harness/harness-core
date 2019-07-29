package io.harness.spotinst;

import io.harness.spotinst.model.ElastiGroup;

import java.util.List;

public interface SpotInstHelperServiceDelegate {
  List<ElastiGroup> listAllElastiGroups(String spotInstToken, String spotInstAccountId, String elastiGroupNamePrefix)
      throws Exception;
  void createElastiGroup(String spotInstToken, String spotInstAccountId, String jsonPayload) throws Exception;
  void deleteElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception;
}