package software.wings.service.intfc.yaml;

import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.yaml.YamlPayload;

/**
 * Service Yaml Resource Service.
 *
 * @author bsollish
 */
public interface ServiceYamlResourceService {
  /**
   * Gets the yaml version of a service by serviceId
   *
   * @param appId  the app id
   * @param serviceId  the service id
   * @return the rest response
   */
  public RestResponse<YamlPayload> getService(String appId, String serviceId);

  /**
   * Update a service that is sent as Yaml (in a JSON "wrapper")
   *
   * @param serviceId  the service id
   * @param yamlPayload the yaml version of service
   * @return the rest response
   */
  public RestResponse<Service> updateService(
      String appId, String serviceId, YamlPayload yamlPayload, boolean deleteEnabled);
}
