package software.wings.utils;

import static io.harness.validation.Validator.notNullCheck;

import io.harness.exception.InvalidRequestException;

import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.GitConfigHelperService;
import software.wings.service.intfc.SettingsService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GitUtils {
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
}
