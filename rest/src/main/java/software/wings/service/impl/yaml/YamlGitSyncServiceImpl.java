package software.wings.service.impl.yaml;

import software.wings.beans.RestResponse;
import software.wings.service.intfc.yaml.YamlGitSyncService;
import software.wings.yaml.gitSync.YamlGitSync;

public class YamlGitSyncServiceImpl implements YamlGitSyncService {
  /**
   * Gets the yaml git sync info by object type and entitytId (uuid)
   *
   * @param type the object type
   * @param entityId the uuid of the entity
   * @param accountId the account id
   * @return the rest response
   */
  public RestResponse<YamlGitSync> getYamlGitSync(String type, String entityId, String accountId) {
    RestResponse rr = new RestResponse<>();

    rr.setResource(new YamlGitSync());

    return rr;
  }

  /**
   * Creates a new yaml git sync info by object type and entitytId (uuid)
   *
   * @param type the object type
   * @param accountId the account id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  public RestResponse<YamlGitSync> saveYamlGitSync(String type, String accountId, YamlGitSync yamlGitSync) {
    RestResponse rr = new RestResponse<>();

    rr.setResource(new YamlGitSync());

    return rr;
  }

  /**
   * Updates the yaml git sync info by object type and entitytId (uuid)
   *
   * @param type the object type
   *@param entityId the uuid of the entity
   * @param accountId the account id
   * @param yamlGitSync the yamlGitSync info
   * @return the rest response
   */
  public RestResponse<YamlGitSync> updateYamlGitSync(
      String type, String entityId, String accountId, YamlGitSync yamlGitSync) {
    RestResponse rr = new RestResponse<>();

    rr.setResource(new YamlGitSync());

    return rr;
  }
}
