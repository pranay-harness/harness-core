/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.utils;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.StringUtils;

@Singleton
@OwnedBy(CDP)
@TargetModule(_870_CG_ORCHESTRATION)
public class GitUtilsManager {
  @Inject private GitConfigHelperService gitConfigHelperService;
  @Inject private SettingsService settingsService;
  public GitConfig getGitConfig(String sourceRepoSettingId) {
    SettingAttribute gitSettingAttribute = settingsService.get(sourceRepoSettingId);
    notNullCheck("Git connector not found", gitSettingAttribute);
    if (!(gitSettingAttribute.getValue() instanceof GitConfig)) {
      throw new InvalidRequestException("Invalid Git Repo");
    }

    GitConfig gitConfig = (GitConfig) gitSettingAttribute.getValue();
    gitConfigHelperService.setSshKeySettingAttributeIfNeeded(gitConfig);
    return gitConfig;
  }

  /* To use method GitConfigHelperService#getRepositoryUrl */
  @Deprecated
  public static String fetchCompleteGitRepoUrl(GitConfig gitConfig, String repoName) {
    if (GitConfig.UrlType.ACCOUNT == gitConfig.getUrlType()) {
      if (StringUtils.isEmpty(repoName)) {
        throw new InvalidRequestException("Repo name cannot be null for Account level git connector");
      }
      String purgedRepoUrl = gitConfig.getRepoUrl().replaceAll("/*$", "");
      String purgedRepoName = repoName.replaceAll("^/*", "");
      return purgedRepoUrl + "/" + purgedRepoName;
    } else {
      return gitConfig.getRepoUrl();
    }
  }
}
