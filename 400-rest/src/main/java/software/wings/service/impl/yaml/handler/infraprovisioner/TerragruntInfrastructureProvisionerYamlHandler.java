/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SecretManagerConfig;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.InfrastructureProvisionerType;
import software.wings.beans.TerragruntInfrastructureProvisioner;
import software.wings.beans.TerragruntInfrastructureProvisioner.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDP)
@TargetModule(HarnessModule._955_CG_YAML)
public class TerragruntInfrastructureProvisionerYamlHandler
    extends InfrastructureProvisionerYamlHandler<Yaml, TerragruntInfrastructureProvisioner> {
  @Inject SecretManager secretManager;

  @Override
  public Yaml toYaml(TerragruntInfrastructureProvisioner bean, String appId) {
    Yaml yaml = Yaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setType(InfrastructureProvisionerType.TERRAGRUNT.name());
    yaml.setPath(bean.getPath());
    yaml.setSourceRepoSettingName(getSourceRepoSettingName(appId, bean.getSourceRepoSettingId()));
    yaml.setSourceRepoBranch(bean.getSourceRepoBranch());
    yaml.setCommitId(bean.getCommitId());
    yaml.setRepoName(bean.getRepoName());

    if (isNotEmpty(bean.getSecretManagerId())) {
      SecretManagerConfig secretManagerConfig =
          secretManager.getSecretManager(bean.getAccountId(), bean.getSecretManagerId());
      if (isNull(secretManagerConfig)) {
        throw new InvalidRequestException("No secret manager found.", USER);
      }
      yaml.setSecretMangerName(secretManagerConfig.getName());
    }
    yaml.setSkipRefreshBeforeApplyingPlan(bean.isSkipRefreshBeforeApplyingPlan());
    return yaml;
  }

  @Override
  public TerragruntInfrastructureProvisioner upsertFromYaml(
      ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    TerragruntInfrastructureProvisioner current = TerragruntInfrastructureProvisioner.builder().build();
    toBean(current, changeContext, appId);

    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    TerragruntInfrastructureProvisioner previous =
        (TerragruntInfrastructureProvisioner) infrastructureProvisionerService.getByName(appId, name);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      current.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      current = (TerragruntInfrastructureProvisioner) infrastructureProvisionerService.update(current);
    } else {
      current = (TerragruntInfrastructureProvisioner) infrastructureProvisionerService.save(current);
    }

    changeContext.setEntity(current);
    return current;
  }

  private void toBean(TerragruntInfrastructureProvisioner bean, ChangeContext<Yaml> changeContext, String appId) {
    Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    super.toBean(changeContext, bean, appId, yamlFilePath);
    bean.setPath(yaml.getPath());
    bean.setSourceRepoSettingId(getSourceRepoSettingId(appId, yaml.getSourceRepoSettingName()));
    validateBranchCommitId(yaml.getSourceRepoBranch(), yaml.getCommitId());
    bean.setSourceRepoBranch(yaml.getSourceRepoBranch());
    bean.setCommitId(yaml.getCommitId());
    bean.setRepoName(yaml.getRepoName());
    bean.setSkipRefreshBeforeApplyingPlan(yaml.isSkipRefreshBeforeApplyingPlan());

    if (isNotEmpty(yaml.getSecretMangerName())) {
      SecretManagerConfig secretManagerConfig =
          secretManager.getSecretManagerByName(bean.getAccountId(), yaml.getSecretMangerName());
      if (isNull(secretManagerConfig)) {
        throw new InvalidRequestException(
            format("No secret manager found with name: %s.", yaml.getSecretMangerName()), USER);
      }
      bean.setSecretManagerId(secretManagerConfig.getUuid());
    }
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public TerragruntInfrastructureProvisioner get(String accountId, String yamlFilePath) {
    return (TerragruntInfrastructureProvisioner) yamlHelper.getInfrastructureProvisioner(accountId, yamlFilePath);
  }
}
