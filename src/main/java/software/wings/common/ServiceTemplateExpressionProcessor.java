/**
 *
 */

package software.wings.common;

import software.wings.api.ServiceTemplateElement;
import software.wings.beans.ServiceTemplate;
import software.wings.utils.MapperUtils;

/**
 * @author Rishi
 */
public class ServiceTemplateExpressionProcessor {
  static ServiceTemplateElement convertToServiceTemplateElement(ServiceTemplate serviceTemplate) {
    ServiceTemplateElement ste = new ServiceTemplateElement();
    MapperUtils.mapObject(serviceTemplate, ste);

    ste.setServiceElement(ServiceExpressionProcessor.convertToServiceElement(serviceTemplate.getService()));
    return ste;
  }
}
