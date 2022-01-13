package io.harness.ccm.commons.service.intf;

import java.util.List;
import java.util.Map;

public interface EntityMetadataService {
  Map<String, String> getEntityIdToNameMapping(List<String> entityIds, String harnessAccountId, String fieldName);
  Map<String, String> getAccountNamePerAwsAccountId(List<String> awsAccountIds, String harnessAccountId);
}
