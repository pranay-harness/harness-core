package software.wings.infra;

import static io.harness.validation.Validator.ensureType;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.InfrastructureType.AZURE_WEBAPP;

import software.wings.beans.AzureWebAppInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;

@JsonTypeName(AZURE_WEBAPP)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "AzureWebAppInfraKeys")
public class AzureWebAppInfra extends AzureAppServiceInfra {
  private String webApp;

  @Override
  public InfrastructureMapping getInfraMapping() {
    AzureWebAppInfrastructureMapping infrastructureMapping = AzureWebAppInfrastructureMapping.builder()
                                                                 .subscriptionId(subscriptionId)
                                                                 .resourceGroup(resourceGroup)
                                                                 .deploymentSlot(deploymentSlot)
                                                                 .webApp(webApp)
                                                                 .build();
    infrastructureMapping.setComputeProviderSettingId(cloudProviderId);
    return infrastructureMapping;
  }

  @Override
  public Class<? extends InfrastructureMapping> getMappingClass() {
    return AzureWebAppInfrastructureMapping.class;
  }

  @Override
  public String getInfrastructureType() {
    return AZURE_WEBAPP;
  }

  @Override
  protected void setDeploymentSpecificExpression(Map<String, Object> resolvedExpressions) {
    Object webAppName = resolvedExpressions.get("webApp");
    String errorMsg = "Deployment Slot should be of String type";
    notNullCheck(errorMsg, webAppName);
    ensureType(String.class, webAppName, errorMsg);
    setWebApp((String) webAppName);
  }

  @Override
  protected Set<String> getDeploymentSpecificExpression() {
    Set<String> expressions = new HashSet<>();
    expressions.add("webApp");
    return expressions;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName(AZURE_WEBAPP)
  public static final class Yaml extends AzureAppServiceInfraYaml {
    private String webApp;

    @Builder
    public Yaml(String type, String cloudProviderName, String subscriptionId, String resourceGroup,
        String deploymentSlot, String webApp) {
      super(type, cloudProviderName, subscriptionId, resourceGroup, deploymentSlot);
      this.webApp = webApp;
    }

    public Yaml() {
      super(AZURE_WEBAPP);
    }
  }
}
