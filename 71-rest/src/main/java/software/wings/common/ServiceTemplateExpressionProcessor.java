/**
 *
 */

package software.wings.common;

import io.harness.serializer.MapperUtils;
import lombok.experimental.UtilityClass;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;

/**
 * The type Service template expression processor.
 *
 * @author Rishi
 */
@UtilityClass
public class ServiceTemplateExpressionProcessor {
  /**
   * Convert to service template element service template element.
   *
   * @param serviceTemplate the service template
   * @return the service template element
   */
  static ServiceTemplateElement convertToServiceTemplateElement(ServiceTemplate serviceTemplate, Service service) {
    ServiceTemplateElement ste = new ServiceTemplateElement();
    MapperUtils.mapObject(serviceTemplate, ste);

    ste.setServiceElement(ServiceExpressionProcessor.convertToServiceElement(service));
    return ste;
  }
}
