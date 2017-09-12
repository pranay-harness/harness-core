package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.beans.command.ServiceCommand;
import software.wings.yaml.YamlPayload;

/**
 * Yaml Directory Service.
 *
 * @author bsollish
 */
public interface YamlResourceService {
  /**
   * Find by app, service and service command ids.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @return the application
   */
  RestResponse<YamlPayload> getServiceCommand(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String serviceCommandId);

  /**
   * Update by app, service and service command ids and yaml payload
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param serviceCommandId the service command id
   * @param yamlPayload the yaml version of the service command
   * @return the application
   */
  ServiceCommand updateServiceCommand(
      @NotEmpty String appId, @NotEmpty String serviceId, @NotEmpty String serviceCommandId, YamlPayload yamlPayload);
}
