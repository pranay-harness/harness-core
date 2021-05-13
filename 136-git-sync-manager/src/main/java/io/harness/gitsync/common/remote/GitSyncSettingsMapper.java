package io.harness.gitsync.common.remote;

import static io.harness.gitsync.common.beans.GitSyncSettings.IS_EXECUTE_ON_DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitSyncSettings;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class GitSyncSettingsMapper {
  public GitSyncSettingsDTO getDTOFromGitSyncSettings(GitSyncSettings savedGitSyncSettings) {
    return GitSyncSettingsDTO.builder()
        .accountIdentifier(savedGitSyncSettings.getAccountIdentifier())
        .orgIdentifier(savedGitSyncSettings.getOrgIdentifier())
        .projectIdentifier(savedGitSyncSettings.getProjectIdentifier())
        .executeOnDelegate(savedGitSyncSettings.getSettings().get(IS_EXECUTE_ON_DELEGATE).equals(String.valueOf(true)))
        .build();
  }

  public GitSyncSettings getGitSyncSettingsFromDTO(GitSyncSettingsDTO gitSyncSettingsDTO) {
    Map<String, String> settings = new HashMap<>();
    settings.put(IS_EXECUTE_ON_DELEGATE,
        (gitSyncSettingsDTO.isExecuteOnDelegate()) ? String.valueOf(true) : String.valueOf(false));
    return GitSyncSettings.builder()
        .accountIdentifier(gitSyncSettingsDTO.getAccountIdentifier())
        .orgIdentifier(gitSyncSettingsDTO.getOrgIdentifier())
        .projectIdentifier(gitSyncSettingsDTO.getProjectIdentifier())
        .settings(settings)
        .build();
  }
}
