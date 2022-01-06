/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitSyncSettings;

import static io.harness.gitsync.common.beans.GitSyncSettings.IS_EXECUTE_ON_DELEGATE;
import static io.harness.rule.OwnerRule.HARI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.beans.GitSyncSettings;
import io.harness.gitsync.common.dtos.GitSyncSettingsDTO;
import io.harness.gitsync.common.impl.GitSyncSettingsServiceImpl;
import io.harness.repositories.gitSyncSettings.GitSyncSettingsRepository;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.DX)
public class GitSyncSettingsTest extends GitSyncTestBase {
  @Inject GitSyncSettingsServiceImpl gitSyncSettingsService;
  @Inject GitSyncSettingsRepository gitSyncSettingsRepository;
  public static final String accountIdentifier = "accountIdentifier";
  public static final String projectIdentifier = "projectIdentifier";
  public static final String orgIdentifier = "orgIdentifier";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HARI)
  @Category(UnitTests.class)
  public void testUpdateSetting() {
    Map<String, String> settings = new HashMap<>();
    settings.put(IS_EXECUTE_ON_DELEGATE, String.valueOf(true));
    GitSyncSettings gitSyncSettings = GitSyncSettings.builder()
                                          .accountIdentifier(accountIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .orgIdentifier(orgIdentifier)
                                          .settings(settings)
                                          .build();
    gitSyncSettingsRepository.save(gitSyncSettings);
    GitSyncSettingsDTO request = GitSyncSettingsDTO.builder()
                                     .accountIdentifier(accountIdentifier)
                                     .organizationIdentifier(orgIdentifier)
                                     .projectIdentifier(projectIdentifier)
                                     .executeOnDelegate(false)
                                     .build();
    final GitSyncSettingsDTO update = gitSyncSettingsService.update(request);
    assertThat(update.equals(request));
    final Optional<GitSyncSettingsDTO> gitSyncSettingsDTOOptional =
        gitSyncSettingsService.get(accountIdentifier, orgIdentifier, projectIdentifier);
    Boolean isExecuteOnDelegate = gitSyncSettingsDTOOptional.get().isExecuteOnDelegate();
    assertThat(isExecuteOnDelegate.equals(false));
  }
}
