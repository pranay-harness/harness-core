/**
 *
 */

package software.wings.common;

import com.google.inject.Singleton;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.InstanceElement;
import software.wings.beans.ServiceVariable;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.WorkflowStandardParams;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;

/**
 * The type Variable processor.
 */
@Singleton
public class VariableProcessor {
  @Inject @Transient private ServiceTemplateService serviceTemplateService;

  /**
   * Gets variables.
   *
   * @param contextElements the context elements
   * @param workflowExecutionId
   * @return the variables
   */
  public Map<String, String> getVariables(ArrayDeque<ContextElement> contextElements, String workflowExecutionId) {
    Map<String, String> variables = new HashMap<>();
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
      List<ServiceVariable> serviceSettingMap = serviceTemplateService.computeServiceVariables(standardParam.getAppId(),
          standardParam.getEnvId(), instance.getServiceTemplateElement().getUuid(), workflowExecutionId);
      serviceSettingMap.forEach(
          serviceVariable -> variables.put(serviceVariable.getName(), new String(serviceVariable.getValue())));
    }

    return variables;
  }
}
