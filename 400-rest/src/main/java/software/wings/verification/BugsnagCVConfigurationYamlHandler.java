/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.bugsnag.BugsnagApplication;
import software.wings.service.intfc.analysis.LogVerificationService;
import software.wings.sm.StateType;
import software.wings.verification.log.BugsnagCVConfiguration;
import software.wings.verification.log.BugsnagCVConfigurationYaml;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration.LogsCVConfigurationYaml;

import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Pranjal on 04/16/2019
 */
@Slf4j
public class BugsnagCVConfigurationYamlHandler extends LogsCVConfigurationYamlHandler {
  @Inject private LogVerificationService logVerificationService;

  @Override
  public LogsCVConfigurationYaml toYaml(LogsCVConfiguration bean, String appId) {
    if (!(bean instanceof BugsnagCVConfiguration)) {
      throw new WingsException("Unexpected type of configuration");
    }
    final BugsnagCVConfigurationYaml yaml = (BugsnagCVConfigurationYaml) super.toYaml(bean, appId);
    BugsnagCVConfiguration bugsnagCVConfiguration = (BugsnagCVConfiguration) bean;

    try {
      Set<BugsnagApplication> orgs =
          logVerificationService.getOrgProjectListBugsnag(bean.getConnectorId(), "", StateType.BUG_SNAG, false);
      BugsnagApplication bugsnagOrganisation = null;
      for (BugsnagApplication org : orgs) {
        if (String.valueOf(org.getId()).equals(((BugsnagCVConfiguration) bean).getOrgId())) {
          yaml.setOrgName(org.getName());
          bugsnagOrganisation = org;
          break;
        }
      }
      if (bugsnagOrganisation != null) {
        Set<BugsnagApplication> projects = logVerificationService.getOrgProjectListBugsnag(
            bean.getConnectorId(), bugsnagOrganisation.getId(), StateType.BUG_SNAG, true);
        for (BugsnagApplication project : projects) {
          if (String.valueOf(project.getId()).equals(((BugsnagCVConfiguration) bean).getProjectId())) {
            yaml.setProjectName(project.getName());
            break;
          }
        }
      }
      if (isEmpty(yaml.getOrgName()) || isEmpty(yaml.getProjectName())) {
        final String errMsg = String.format(
            "Bugsnag Organization Name or Project Name is empty during conversion to yaml. OrganizationId %s, ProjectId %s",
            ((BugsnagCVConfiguration) bean).getOrgId(), ((BugsnagCVConfiguration) bean).getProjectId());
        log.error(errMsg);
        throw new InvalidRequestException(errMsg);
      }
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }

    yaml.setBrowserApplication(bugsnagCVConfiguration.isBrowserApplication());
    yaml.setReleaseStage(bugsnagCVConfiguration.getReleaseStage());
    yaml.setQuery(bugsnagCVConfiguration.getQuery());
    return yaml;
  }

  @Override
  public BugsnagCVConfiguration upsertFromYaml(
      ChangeContext<LogsCVConfigurationYaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);

    notNullCheck("Couldn't retrieve app from yaml:" + yamlFilePath, appId, USER);

    String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
    String name = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());

    CVConfiguration previous = cvConfigurationService.getConfiguration(name, appId, envId);

    BugsnagCVConfiguration bean = cvConfigurationService.getConfiguration(name, appId, envId);
    toBean(bean, changeContext, appId);

    if (previous != null) {
      bean.setUuid(previous.getUuid());
      cvConfigurationService.updateConfiguration(bean, appId);
    } else {
      cvConfigurationService.saveToDatabase(bean, true);
    }
    return bean;
  }

  @Override
  public Class getYamlClass() {
    return BugsnagCVConfigurationYaml.class;
  }

  @Override
  public BugsnagCVConfiguration get(String accountId, String yamlFilePath) {
    return (BugsnagCVConfiguration) yamlHelper.getCVConfiguration(accountId, yamlFilePath);
  }
  private void toBean(BugsnagCVConfiguration bean, ChangeContext<LogsCVConfigurationYaml> changeContext, String appId) {
    String accountId = changeContext.getChange().getAccountId();
    BugsnagCVConfigurationYaml yaml = (BugsnagCVConfigurationYaml) changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    SettingAttribute bugsnagConnector = settingsService.getSettingAttributeByName(accountId, yaml.getConnectorName());

    super.toBean(changeContext, bean, appId, yamlFilePath);
    try {
      Set<BugsnagApplication> orgs =
          logVerificationService.getOrgProjectListBugsnag(bugsnagConnector.getUuid(), "", StateType.BUG_SNAG, false);
      BugsnagApplication bugsnagOrganisation = null;
      for (BugsnagApplication org : orgs) {
        if (org.getName().equals(yaml.getOrgName())) {
          bean.setOrgId(String.valueOf(org.getId()));
          bugsnagOrganisation = org;
          break;
        }
      }
      if (bugsnagOrganisation != null) {
        Set<BugsnagApplication> projects = logVerificationService.getOrgProjectListBugsnag(
            bugsnagConnector.getUuid(), bugsnagOrganisation.getId(), StateType.BUG_SNAG, true);
        for (BugsnagApplication project : projects) {
          if (project.getName().equals(yaml.getProjectName())) {
            bean.setProjectId(String.valueOf(project.getId()));
            break;
          }
        }
      }
      if (bugsnagOrganisation == null) {
        final String errMsg = String.format(
            "Bugsnag Organization Name or Project Name is incorrect during edit from yaml. Organization Name %s, Project Name %s",
            yaml.getOrgName(), yaml.getProjectName());
        log.error(errMsg);
        throw new WingsException(errMsg);
      }
    } catch (Exception ex) {
      throw new WingsException(ex.getMessage());
    }
    bean.setBrowserApplication(yaml.isBrowserApplication());
    bean.setReleaseStage(yaml.getReleaseStage());
    bean.setStateType(StateType.BUG_SNAG);
    bean.setQuery(yaml.getQuery());
  }
}
