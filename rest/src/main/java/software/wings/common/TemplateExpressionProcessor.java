package software.wings.common;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.exception.WingsException;
import software.wings.expression.ExpressionEvaluator;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.utils.Misc;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Created by sgurubelli on 8/16/17.
 */
@Singleton
public class TemplateExpressionProcessor {
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;

  public static String changeToWorkflowVariable(TemplateExpression templateExpression) {
    String templateVariable = templateExpression.getExpression();
    Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(templateVariable);
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

  /**
   * Resolves Inframapping Template expression
   *
   * @param context
   * @param templateExpression
   * @return InfraStructureMapping
   */
  public InfrastructureMapping resolveInfraMapping(
      ExecutionContext context, String appId, TemplateExpression templateExpression) {
    try {
      String infraMappingId = resolveTemplateExpression(context, templateExpression);
      InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
      if (infrastructureMapping == null) {
        throw new WingsException("Service Infrastructure expression  " + templateExpression + " resolved as"
            + infraMappingId + ". However, no Service Infrastructure found id : " + infraMappingId);
      }
      return infrastructureMapping;

    } catch (Exception ex) {
      throw new WingsException("Failed to resolve the Service Infrastructure expression:" + templateExpression
          + "Reason: " + Misc.getMessage(ex));
    }
  }

  /**
   * Resolve service template expression
   *
   * @param context
   * @param app
   * @param templateExpression
   * @return Service
   */
  public Service resolveService(ExecutionContext context, Application app, TemplateExpression templateExpression) {
    try {
      String serviceId = resolveTemplateExpression(context, templateExpression);
      // Then variable contains serviceId
      Service service = serviceResourceService.get(app.getAppId(), serviceId, false);
      if (service == null) {
        throw new WingsException("Service expression " + templateExpression.getExpression() + " resolved as" + serviceId
            + ". However, no service found with service name: " + serviceId);
      }
      return service;

    } catch (Exception ex) {
      throw new WingsException(
          "Failed to resolve the service expression:" + templateExpression + "Reason: " + Misc.getMessage(ex));
    }
  }

  /**
   * Resolve SettingAttribute template expression
   *
   * @param context
   * @param templateExpression
   * @return Setting attribute
   */
  public SettingAttribute resolveSettingAttribute(ExecutionContext context, TemplateExpression templateExpression) {
    try {
      String settingId = resolveTemplateExpression(context, templateExpression);
      SettingAttribute settingAttribute = settingsService.get(settingId);
      if (settingAttribute == null) {
        throw new WingsException("Setting expression " + templateExpression.getExpression() + " resolved as" + settingId
            + ". However, no Connector found with id : " + settingId);
      }
      return settingAttribute;

    } catch (Exception ex) {
      throw new WingsException(
          "Failed to resolve the service expression:" + templateExpression + "Reason: " + Misc.getMessage(ex));
    }
  }

  /**
   * Resolves the template expression
   * @param context
   * @param templateExpression
   * @return
   */
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
