/**
 *
 */

package software.wings.common;

import software.wings.api.ServiceTemplateElement;
import software.wings.beans.ServiceTemplate;
import software.wings.utils.MapperUtils;

/**
 * The type Service template expression processor.
 *
 * @author Rishi
 */
public class ServiceTemplateExpressionProcessor {
  /**
   * Convert to service template element service template element.
   *
   * @param serviceTemplate the service template
   * @return the service template element
   */
  static ServiceTemplateElement convertToServiceTemplateElement(ServiceTemplate serviceTemplate) {
    ServiceTemplateElement ste = new ServiceTemplateElement();
    MapperUtils.mapObject(serviceTemplate, ste);

    ste.setServiceElement(ServiceExpressionProcessor.convertToServiceElement(serviceTemplate.getService()));
    return ste;
  }
}
