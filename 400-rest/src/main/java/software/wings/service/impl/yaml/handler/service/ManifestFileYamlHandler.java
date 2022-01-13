/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.service;

import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.WingsException;

import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.ManifestFile.Yaml;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ManifestFileYamlHandler extends BaseYamlHandler<Yaml, ManifestFile> {
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject ApplicationManifestService applicationManifestService;

  @Override
  public Yaml toYaml(ManifestFile manifestFile, String appId) {
    return Yaml.builder().harnessApiVersion(getHarnessApiVersion()).fileContent(manifestFile.getFileContent()).build();
  }

  @Override
  public ManifestFile upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("appId null for given yaml file:" + yamlFilePath, appId, USER);

    ApplicationManifest applicationManifest = yamlHelper.getApplicationManifest(appId, yamlFilePath);
    notNullCheck("ApplicationManifest null for given yaml file:" + yamlFilePath, applicationManifest, USER);

    String fileName = getActualFileName(yamlFilePath, changeContext.getYamlType());
    ManifestFile previous = yamlHelper.getManifestFile(appId, yamlFilePath, fileName);

    ManifestFile manifestFile = toBean(changeContext, fileName);
    manifestFile.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    boolean isCreate = true;
    if (previous != null) {
      manifestFile.setUuid(previous.getUuid());
      isCreate = false;
    }

    return applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, isCreate);
  }

  /**
   * Remove path prefix, Setup/Application/....Manifests/Files/ and return actual file name
   * @param yamlFilePath
   * @return
   */
  private String getActualFileName(String yamlFilePath, YamlType yamlType) {
    Pattern pattern = Pattern.compile(yamlType.getPrefixExpression());
    Matcher matcher = pattern.matcher(yamlFilePath);
    String prefix = matcher.find() ? matcher.group(0) : null;

    if (prefix == null) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message",
              "Invalid YamlFile Path for Manifest File, required Prefix not present in Path: " + yamlFilePath);
    }

    return yamlFilePath.substring(prefix.length());
  }

  private ManifestFile toBean(ChangeContext<Yaml> changeContext, String fileName) {
    Change change = changeContext.getChange();

    String yamlFilePath = changeContext.getChange().getFilePath();

    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), yamlFilePath);
    notNullCheck("Could not lookup app for the yaml file: " + yamlFilePath, appId, USER);

    ApplicationManifest applicationManifest = yamlHelper.getApplicationManifest(appId, yamlFilePath);
    notNullCheck("Application Manifest null for given yaml file:" + yamlFilePath, applicationManifest, USER);

    ManifestFile manifestFile = ManifestFile.builder()
                                    .fileName(fileName)
                                    .fileContent(change.getFileContent())
                                    .applicationManifestId(applicationManifest.getUuid())
                                    .build();
    manifestFile.setAppId(appId);
    return manifestFile;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public ManifestFile get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("Could not find Application for the yaml file: " + yamlFilePath, appId, USER);

    ApplicationManifest applicationManifest = yamlHelper.getApplicationManifest(appId, yamlFilePath);
    notNullCheck("Application Manifest null for given yaml file:" + yamlFilePath, applicationManifest, USER);

    YamlType yamlType = yamlHelper.getApplicationManifestBasedYamlTypeForFilePath(yamlFilePath);
    return applicationManifestService.getManifestFileByFileName(
        applicationManifest.getUuid(), getActualFileName(yamlFilePath, yamlType));
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    Change change = changeContext.getChange();
    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());

    ManifestFile manifestFile = get(change.getAccountId(), change.getFilePath());
    if (manifestFile != null) {
      manifestFile.setSyncFromGit(changeContext.getChange().isSyncFromGit());
      applicationManifestService.deleteManifestFile(appId, manifestFile);
    }
  }
}
