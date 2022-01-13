/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitopsprovider.mappers;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;
import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.delegate.beans.connector.gitops.ConnectedArgoGitOpsInfoDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.GitOpsProviderTestBase;
import io.harness.gitopsprovider.entity.ConnectedArgoProvider;
import io.harness.gitopsprovider.entity.GitOpsProvider;
import io.harness.gitopsprovider.utils.TestConstants;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(GITOPS)
public class ConnectedGitOpsProviderEntityMapperTest extends GitOpsProviderTestBase {
  @Inject private Map<GitOpsProviderType, GitOpsProviderEntityMapper> gitopsProviderEntityMapperBinding;
  private ConnectedGitOpsProviderEntityMapper target;

  @Before
  public void setUp() throws Exception {
    target = (ConnectedGitOpsProviderEntityMapper) gitopsProviderEntityMapperBinding.get(
        GitOpsProviderType.CONNECTED_ARGO_PROVIDER);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void toGitOpsProvider() {
    GitOpsProviderDTO dto =
        GitOpsProviderDTO.builder()
            .tags(ImmutableMap.of("k1", "", "k2", "v2"))
            .projectIdentifier(TestConstants.PROJECT_ID)
            .orgIdentifier(TestConstants.ORG_ID)
            .description(TestConstants.DESCRIPTION)
            .identifier(TestConstants.IDENTIFIER)
            .name(TestConstants.ARGO_NAME)
            .infoDTO(ConnectedArgoGitOpsInfoDTO.builder().adapterUrl(TestConstants.ADAPTER_URL).build())
            .build();

    final GitOpsProvider gitOpsProvider = target.toGitOpsProviderEntity(dto, TestConstants.ACCOUNT_IDENTIFIER);

    assertThat(gitOpsProvider.getGitOpsProviderType()).isEqualTo(GitOpsProviderType.CONNECTED_ARGO_PROVIDER);
    assertThat(gitOpsProvider.getName()).isEqualTo(TestConstants.ARGO_NAME);
    assertThat(gitOpsProvider.getIdentifier()).isEqualTo(TestConstants.IDENTIFIER);
    assertThat(gitOpsProvider.getProjectIdentifier()).isEqualTo(TestConstants.PROJECT_ID);
    assertThat(gitOpsProvider.getOrgIdentifier()).isEqualTo(TestConstants.ORG_ID);
    assertThat(gitOpsProvider.getAccountIdentifier()).isEqualTo(TestConstants.ACCOUNT_IDENTIFIER);
    assertThat(gitOpsProvider.getTags())
        .containsExactly(NGTag.builder().key("k1").value("").build(), NGTag.builder().key("k2").value("v2").build());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void toGitOpsProviderEntity() {
    GitOpsProvider entity = ConnectedArgoProvider.builder().adapterUrl(TestConstants.ADAPTER_URL).build();
    entity.setTags(
        Arrays.asList(NGTag.builder().key("k1").value("").build(), NGTag.builder().key("k2").value("v2").build()));
    entity.setName(TestConstants.ARGO_NAME);
    entity.setDescription(TestConstants.DESCRIPTION);
    entity.setIdentifier(TestConstants.IDENTIFIER);
    entity.setProjectIdentifier(TestConstants.PROJECT_ID);
    entity.setOrgIdentifier(TestConstants.ORG_ID);

    final GitOpsProviderResponseDTO dto = target.toGitOpsProviderDTO(entity);

    assertThat(dto.getInfoDTO().getGitProviderType()).isEqualTo(GitOpsProviderType.CONNECTED_ARGO_PROVIDER);
    assertThat(((ConnectedArgoGitOpsInfoDTO) dto.getInfoDTO()).getAdapterUrl()).isEqualTo(TestConstants.ADAPTER_URL);
    assertThat(dto.getName()).isEqualTo(TestConstants.ARGO_NAME);
    assertThat(dto.getIdentifier()).isEqualTo(TestConstants.IDENTIFIER);
    assertThat(dto.getProjectIdentifier()).isEqualTo(TestConstants.PROJECT_ID);
    assertThat(dto.getOrgIdentifier()).isEqualTo(TestConstants.ORG_ID);
    assertThat(dto.getTags()).hasSize(2);
    assertThat(dto.getTags().get("k1")).isEqualTo("");
    assertThat(dto.getTags().get("k2")).isEqualTo("v2");
  }
}
