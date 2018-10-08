package software.wings.common;

import static software.wings.expression.ManagerExpressionEvaluator.matchesVariablePattern;
import static software.wings.expression.ManagerExpressionEvaluator.wingsVariablePattern;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.regex.Matcher;

@Singleton
public class TemplateExpressionProcessor {
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;

  public static String changeToWorkflowVariable(TemplateExpression templateExpression) {
    String templateVariable = templateExpression.getExpression();
    Matcher matcher = wingsVariablePattern.matcher(templateVariable);
    if (matcher.matches()) {
      templateVariable = matcher.group(0);
      templateVariable = templateVariable.substring(2, templateVariable.length() - 1);
      if (!templateVariable.startsWith("workflow.variables.")) {
        templateVariable = "${workflow.variables." + templateVariable + "}";
      }
    } else {
      throw new WingsException("Invalid template expression: " + templateExpression);
    }
    return templateVariable;
  }

  public InfrastructureMapping resolveInfraMapping(
      ExecutionContext context, String appId, TemplateExpression templateExpression) {
    String infraMappingId = resolveTemplateExpression(context, templateExpression);
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (infrastructureMapping == null) {
      if (matchesVariablePattern(infraMappingId)) {
        throw new WingsException("No value provided for templated Service Infrastructure workflow variable ["
                + templateExpression.getExpression() + "]",
            WingsException.USER);
      }
      throw new WingsException("Service Infrastructure expression  " + templateExpression.getExpression()
              + " resolved as [" + infraMappingId
              + "]. However, no Service Infrastructure found with InframappingId : " + infraMappingId,
          WingsException.USER);
    }
    return infrastructureMapping;
  }

  public Service resolveService(ExecutionContext context, Application app, TemplateExpression templateExpression) {
    String serviceId = resolveTemplateExpression(context, templateExpression);
    // Then variable contains serviceId
    Service service = serviceResourceService.get(app.getAppId(), serviceId, false);
    if (service == null) {
      if (matchesVariablePattern(serviceId)) {
        throw new WingsException(
            "No value provided for templated Service workflow variable [" + templateExpression.getExpression() + "]",
            WingsException.USER);
      }
      throw new WingsException("Service templated workflow variable " + templateExpression.getExpression()
              + " resolved as [" + serviceId + "]. However, no service found with service id: " + serviceId,
          WingsException.USER);
    }
    return service;
  }

  public SettingAttribute resolveSettingAttribute(ExecutionContext context, TemplateExpression templateExpression) {
    String settingId = resolveTemplateExpression(context, templateExpression);
    SettingAttribute settingAttribute = settingsService.get(settingId);
    if (settingAttribute == null) {
      if (matchesVariablePattern(settingId)) {
        throw new WingsException(
            "No value provided for templated Setting workflow variable [" + templateExpression.getExpression() + "]",
            WingsException.USER);
      }
      throw new WingsException("Setting expression " + templateExpression.getExpression() + " resolved as [" + settingId
          + "]. However, no Connector found with id : " + settingId);
    }
    return settingAttribute;
  }

  public String resolveTemplateExpression(ExecutionContext context, TemplateExpression templateExpression) {
    String expression = changeToWorkflowVariable(templateExpression);
    return context.renderExpression(expression);
  }

  public TemplateExpression getTemplateExpression(List<TemplateExpression> templateExpressions, String fieldName) {
    return templateExpressions.stream()
        .filter(templateExpression -> templateExpression.getFieldName().equals(fieldName))
        .findFirst()
        .orElse(null);
  }

  public static boolean checkFieldTemplatized(String fieldName, List<TemplateExpression> templateExpressions) {
    if (templateExpressions == null) {
      return false;
    }
    return templateExpressions.stream().anyMatch(
        templateExpression -> templateExpression.getFieldName().equals(fieldName));
  }
}
