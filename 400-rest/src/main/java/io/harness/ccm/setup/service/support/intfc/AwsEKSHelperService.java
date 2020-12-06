package io.harness.ccm.setup.service.support.intfc;

import software.wings.beans.AwsCrossAccountAttributes;

import java.util.List;

public interface AwsEKSHelperService {
  List<String> listEKSClusters(String region, AwsCrossAccountAttributes awsCrossAccountAttributes);

  boolean verifyAccess(String region, AwsCrossAccountAttributes awsCrossAccountAttributes);
}
