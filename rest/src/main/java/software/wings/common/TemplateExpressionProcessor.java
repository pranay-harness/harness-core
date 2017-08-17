package software.wings.common;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Category;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import software.wings.beans.Application;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.dl.PageRequest;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

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
   * @param expression
   * @return  InfraStructureMapping
   */
  public InfrastructureMapping resolveInfraMapping(
      ExecutionContext context, Application app, String serviceId, String expression) {
    try {
      String displayName = context.renderExpression(expression);
      PageRequest<InfrastructureMapping> pageRequest =
          aPageRequest().addFilter("appId", EQ, app.getUuid()).addFilter("serviceId", EQ, serviceId).build();
      List<InfrastructureMapping> infraMappings = infrastructureMappingService.list(pageRequest);
      if (infraMappings == null || infraMappings.isEmpty()) {
        return null;
      }
      Optional<InfrastructureMapping> infraMapping =
          infraMappings.stream().filter(infrastructureMapping -> infrastructureMapping.equals(displayName)).findFirst();
      if (infraMapping.isPresent()) {
        return infraMapping.get();
      }
      throw new WingsException("Service Infrastructure " + expression + " resolved as" + displayName
          + ". However, no Service Infrastructure found with name: " + displayName);
    } catch (Exception ex) {
      throw new WingsException("Failed to resolve the Service Infrastructure expression: " + expression);
    }
  }

  /**
   * Resolve service template expression
   * @param context
   * @param app
   * @param expression
   * @return Service
   */
  public Service resolveService(ExecutionContext context, Application app, String expression) {
    try {
      String serviceName = context.renderExpression(expression);
      PageRequest<Service> pageRequest =
          aPageRequest().addFilter("appId", EQ, app.getUuid()).addFilter("name", EQ, serviceName).build();
      List<Service> services = serviceResourceService.list(pageRequest, false, false);
      if (services != null && !services.isEmpty()) {
        return services.get(0);
      }
      throw new WingsException("Service expression " + expression + " resolved as" + serviceName
          + ". However, no service found with service name: " + serviceName);
    } catch (Exception ex) {
      throw new WingsException("Failed to resolve the service expression: " + expression);
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
    String displayName = context.renderExpression(expression);
    PageRequest<SettingAttribute> pageRequest = aPageRequest()
                                                    .addFilter("accountId", EQ, accountId)
                                                    .addFilter("category", EQ, category)
                                                    .addFilter("name", EQ, displayName)
                                                    .build();

    List<SettingAttribute> settingAttributes = settingsService.list(pageRequest);
    if (settingAttributes == null || settingAttributes.isEmpty()) {
      return null;
    }
    Optional<SettingAttribute> settingAttribute = settingAttributes.stream().findAny();
    if (settingAttribute.isPresent()) {
      return settingAttribute.get();
    }
    return null;
  }
}
