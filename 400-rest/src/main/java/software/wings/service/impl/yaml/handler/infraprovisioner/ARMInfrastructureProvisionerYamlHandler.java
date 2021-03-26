package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static org.apache.commons.lang3.StringUtils.trim;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ARMInfrastructureProvisioner;
import software.wings.beans.ARMInfrastructureProvisionerYaml;
import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

@OwnedBy(CDP)
public class ARMInfrastructureProvisionerYamlHandler
    extends InfrastructureProvisionerYamlHandler<ARMInfrastructureProvisionerYaml, ARMInfrastructureProvisioner> {
  @Override
  public ARMInfrastructureProvisionerYaml toYaml(ARMInfrastructureProvisioner bean, String appId) {
    ARMInfrastructureProvisionerYaml yaml = ARMInfrastructureProvisionerYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureProvisionerType.ARM.name());
    yaml.setResourceType(bean.getResourceType());
    yaml.setScopeType(bean.getScopeType());
    yaml.setSourceType(bean.getSourceType());
    yaml.setTemplateBody(trim(bean.getTemplateBody()));
    yaml.setGitFileConfig(bean.getGitFileConfig());
    return yaml;
  }

  @Override
  public ARMInfrastructureProvisioner upsertFromYaml(
      ChangeContext<ARMInfrastructureProvisionerYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    ARMInfrastructureProvisioner current = ARMInfrastructureProvisioner.builder().build();
    toBean(current, changeContext, appId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    ARMInfrastructureProvisioner previous =
        (ARMInfrastructureProvisioner) infrastructureProvisionerService.getByName(appId, name);

    current.setSyncFromGit(changeContext.getChange().isSyncFromGit());
    if (previous != null) {
      current.setUuid(previous.getUuid());
      current = (ARMInfrastructureProvisioner) infrastructureProvisionerService.update(current);
    } else {
      current = (ARMInfrastructureProvisioner) infrastructureProvisionerService.save(current);
    }

    changeContext.setEntity(current);
    return current;
  }

  private void toBean(
      ARMInfrastructureProvisioner bean, ChangeContext<ARMInfrastructureProvisionerYaml> changeContext, String appId) {
    ARMInfrastructureProvisionerYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setScopeType(yaml.getScopeType());
    bean.setResourceType(yaml.getResourceType());
    bean.setSourceType(yaml.getSourceType());
    bean.setTemplateBody(trim(yaml.getTemplateBody()));
    bean.setGitFileConfig(yaml.getGitFileConfig());
  }

  @Override
  public Class getYamlClass() {
    return ARMInfrastructureProvisionerYaml.class;
  }

  @Override
  public ARMInfrastructureProvisioner get(String accountId, String yamlFilePath) {
    return (ARMInfrastructureProvisioner) yamlHelper.getInfrastructureProvisioner(accountId, yamlFilePath);
  }
}
