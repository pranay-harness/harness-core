package software.wings.service.intfc.yaml.clone;

import software.wings.beans.RestResponse;

public interface YamlCloneService {
  RestResponse cloneEntityUsingYaml(
      String accountId, String appId, boolean includeFiles, String entityType, String entiytId, String newName);
}
