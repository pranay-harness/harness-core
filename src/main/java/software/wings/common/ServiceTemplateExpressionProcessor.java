/**
 *
 */
package software.wings.common;

import org.modelmapper.ModelMapper;
import software.wings.api.ServiceTemplateElement;
import software.wings.beans.ServiceTemplate;

/**
 * @author Rishi
 *
 */
public class ServiceTemplateExpressionProcessor {
  static ServiceTemplateElement convertToServiceTemplateElement(ServiceTemplate serviceTemplate) {
    ModelMapper mm = new ModelMapper();
    ServiceTemplateElement ste = new ServiceTemplateElement();
    mm.map(serviceTemplate, ste);

    ste.setServiceElement(ServiceExpressionProcessor.convertToServiceElement(serviceTemplate.getService()));
    return ste;
  }
}
