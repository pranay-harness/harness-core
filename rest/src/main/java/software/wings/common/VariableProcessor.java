/**
 *
 */

package software.wings.common;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import software.wings.api.InstanceElement;
import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

/**
 * The type Variable processor.
 */
@Singleton
public class VariableProcessor {
  @Inject private ServiceTemplateService serviceTemplateService;

  /**
   * Gets variables.
   *
   * @param contextElements the context elements
   * @return the variables
   */
  public Map<String, String> getVariables(ArrayDeque<ContextElement> contextElements) {
    Map<String, String> variables = Maps.newHashMap();
    Optional<ContextElement> instanceElement =
        contextElements.stream()
            .filter(contextElement -> contextElement.getElementType() == ContextElementType.INSTANCE)
            .findFirst();

    if (instanceElement.isPresent()) {
      WorkflowStandardParams standardParam =
          (WorkflowStandardParams) contextElements.stream()
              .filter(contextElement -> contextElement.getElementType() == ContextElementType.STANDARD)
              .findFirst()
              .get();

      InstanceElement instance = (InstanceElement) instanceElement.get();
      List<ServiceVariable> serviceSettingMap = serviceTemplateService.computeServiceVariables(
          standardParam.getAppId(), standardParam.getEnvId(), instance.getServiceTemplateElement().getUuid());
      serviceSettingMap.forEach(
          serviceVariable -> variables.put(serviceVariable.getName(), new String(serviceVariable.getValue())));
    }

    return variables;
  }
}
