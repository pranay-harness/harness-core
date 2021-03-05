package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisionerYaml;
import software.wings.beans.yaml.ChangeContext;

import java.util.List;

public class ShellScriptProvisionerYamlHandler
    extends InfrastructureProvisionerYamlHandler<ShellScriptInfrastructureProvisionerYaml,
        ShellScriptInfrastructureProvisioner> {
  @Override
  public ShellScriptInfrastructureProvisionerYaml toYaml(ShellScriptInfrastructureProvisioner bean, String appId) {
    ShellScriptInfrastructureProvisionerYaml yaml = ShellScriptInfrastructureProvisionerYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureProvisionerType.SHELL_SCRIPT.name());
    yaml.setScriptBody(bean.getScriptBody());
    return yaml;
  }

  @Override
  public ShellScriptInfrastructureProvisioner upsertFromYaml(
      ChangeContext<ShellScriptInfrastructureProvisionerYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    ShellScriptInfrastructureProvisioner current = ShellScriptInfrastructureProvisioner.builder().build();
    toBean(current, changeContext, appId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    ShellScriptInfrastructureProvisioner previous =
        (ShellScriptInfrastructureProvisioner) infrastructureProvisionerService.getByName(appId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      current.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      current = (ShellScriptInfrastructureProvisioner) infrastructureProvisionerService.update(current);
    } else {
      current = (ShellScriptInfrastructureProvisioner) infrastructureProvisionerService.save(current);
    }

    changeContext.setEntity(current);
    return current;
  }

  private void toBean(ShellScriptInfrastructureProvisioner bean,
      ChangeContext<ShellScriptInfrastructureProvisionerYaml> changeContext, String appId) {
    ShellScriptInfrastructureProvisionerYaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setScriptBody(yaml.getScriptBody());
  }

  @Override
  public ShellScriptInfrastructureProvisioner get(String accountId, String yamlFilePath) {
    return (ShellScriptInfrastructureProvisioner) yamlHelper.getInfrastructureProvisioner(accountId, yamlFilePath);
  }

  @Override
  public Class getYamlClass() {
    return ShellScriptInfrastructureProvisionerYaml.class;
  }
}
