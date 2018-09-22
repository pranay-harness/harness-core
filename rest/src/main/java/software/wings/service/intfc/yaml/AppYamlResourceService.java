package software.wings.service.intfc.yaml;

import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.yaml.YamlPayload;

public interface AppYamlResourceService {
  /**
   * Gets the yaml version of an app by appId
   *
   * @param appId  the app id
   * @return the rest response
   */
  RestResponse<YamlPayload> getApp(String appId);

  /**
   * Update an app that is sent as Yaml (in a JSON "wrapper")
   *
   * @param appId  the app id
   * @param yamlPayload the yaml version of app
   * @return the rest response
   */
  RestResponse<Application> updateApp(String appId, YamlPayload yamlPayload, boolean deleteEnabled);
}
