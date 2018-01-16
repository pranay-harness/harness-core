package software.wings.common;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Category;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.dl.PageRequest;
import software.wings.exception.WingsException;
import software.wings.expression.ExpressionEvaluator;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Created by sgurubelli on 8/16/17.
 */
@Singleton
public class TemplateExpressionProcessor {
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private SettingsService settingsService;

  /**
   * Resolves Inframapping Template expression
   * @param context
   * @param app
   * @param serviceId
   * @param templateExpression
   * @return  InfraStructureMapping
   */
  public InfrastructureMapping resolveInfraMapping(
      ExecutionContext context, Application app, String serviceId, TemplateExpression templateExpression) {
    try {
      String expression = changeToWorkflowVariable(templateExpression);
      String displayNameOrId = context.renderExpression(expression);
      if (templateExpression.getMetadata() != null) {
        if (templateExpression.getMetadata().get(Constants.ENTITY_TYPE) != null) {
          // Then variable contains inframapping id
          return infrastructureMappingService.get(app.getAppId(), displayNameOrId);
        }
      }
      PageRequest<InfrastructureMapping> pageRequest =
          aPageRequest().addFilter("appId", EQ, app.getUuid()).addFilter("serviceId", EQ, serviceId).build();
      List<InfrastructureMapping> infraMappings = infrastructureMappingService.list(pageRequest);
      if (isEmpty(infraMappings)) {
        return null;
      }
      Optional<InfrastructureMapping> infraMapping =
          infraMappings.stream()
              .filter(infrastructureMapping -> infrastructureMapping.equals(displayNameOrId))
              .findFirst();
      if (infraMapping.isPresent()) {
        return infraMapping.get();
      }
      throw new WingsException("Service Infrastructure expression  " + templateExpression + " resolved as"
          + displayNameOrId + ". However, no Service Infrastructure found with name: " + displayNameOrId);
    } catch (Exception ex) {
      throw new WingsException("Failed to resolve the Service Infrastructure expression:" + templateExpression
          + "Reason: " + ex.getMessage());
    }
  }

  /**
   * Resolve service template expression
   * @param context
   * @param app
   * @param templateExpression
   * @return Service
   */
  public Service resolveService(ExecutionContext context, Application app, TemplateExpression templateExpression) {
    try {
      String expression = changeToWorkflowVariable(templateExpression);
      String serviceNameOrId = context.renderExpression(expression);
      if (templateExpression.getMetadata() != null) {
        if (templateExpression.getMetadata().get(Constants.ENTITY_TYPE) != null) {
          // Then variable contains serviceId
          return serviceResourceService.get(app.getAppId(), serviceNameOrId, false);
        }
      }
      PageRequest<Service> pageRequest =
          aPageRequest().addFilter("appId", EQ, app.getUuid()).addFilter("name", EQ, serviceNameOrId).build();
      List<Service> services = serviceResourceService.list(pageRequest, false, false);
      if (isNotEmpty(services)) {
        return services.get(0);
      }
      throw new WingsException("Service expression " + templateExpression.getExpression() + " resolved as"
          + serviceNameOrId + ". However, no service found with service name: " + serviceNameOrId);
    } catch (Exception ex) {
      throw new WingsException(
          "Failed to resolve the service expression:" + templateExpression + "Reason: " + ex.getMessage());
    }
  }

  /**
   * Resolve SettingAttribute template expression
   * @param context
   * @param accountId
   * @param expression
   * @return
   */
  public SettingAttribute resolveSettingAttribute(
      ExecutionContext context, String accountId, String expression, Category category) {
    // expression = changeToWorkflowVariable(expression);
    String displayName = context.renderExpression(expression);
    PageRequest<SettingAttribute> pageRequest = aPageRequest()
                                                    .addFilter("accountId", EQ, accountId)
                                                    .addFilter("category", EQ, category)
                                                    .addFilter("name", EQ, displayName)
                                                    .build();

    List<SettingAttribute> settingAttributes = settingsService.list(pageRequest);
    if (isEmpty(settingAttributes)) {
      return null;
    }
    Optional<SettingAttribute> settingAttribute = settingAttributes.stream().findAny();
    if (settingAttribute.isPresent()) {
      return settingAttribute.get();
    }
    return null;
  }

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
}
