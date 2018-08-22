package software.wings.service.intfc.aws.manager;

import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;

import java.util.List;

public interface AwsCFHelperServiceManager {
  List<AwsCFTemplateParamsData> getParamsData(String type, String data, String awsConfigId, String region);
}