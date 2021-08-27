package io.harness.ccm.commons.service.intf;

import java.util.List;
import java.util.Map;

public interface EntityMetadataService {
  Map<String,String> getAccountNamePerAwsAccountId(List<String> awsAccountIds, String harnessAccountId);
}
