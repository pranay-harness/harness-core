package software.wings.service.intfc.yaml;

import software.wings.beans.RestResponse;
import software.wings.yaml.SetupYaml;
import software.wings.yaml.YamlPayload;

public interface SetupYamlResourceService {
  /**
   * Gets the setup yaml by accountId
   *
   * @param accountId  the account id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getSetup(String accountId);

  /**
   * Update setup that is sent as Yaml (in a JSON "wrapper")
   *
   * @param accountId  the account id
   * @param yamlPayload the yaml version of setup
   * @return the rest response
   */
  public RestResponse<SetupYaml> updateSetup(String accountId, YamlPayload yamlPayload, boolean deleteEnabled);
}
