/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitopsprovider.services;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.gitops.ConnectedArgoGitOpsInfoDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.GitOpsProviderTestBase;
import io.harness.gitopsprovider.utils.TestConstants;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class GitopsProviderServiceTest extends GitOpsProviderTestBase {
  @Inject private GitopsProviderService gitopsProviderService;

  private final ImmutableMap<String, String> TAGS = ImmutableMap.of("k1", "", "k2", "v2");

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testSave() {
    final GitOpsProviderDTO inputDTO = buildConnectedGitOpsDTO("prefix");
    final GitOpsProviderResponseDTO savedDTO = gitopsProviderService.create(inputDTO, TestConstants.ACCOUNT_IDENTIFIER);
    match(inputDTO, savedDTO);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGet() {
    final GitOpsProviderDTO inputDTO = buildConnectedGitOpsDTO("prefix");
    final GitOpsProviderResponseDTO savedDTO = gitopsProviderService.create(inputDTO, TestConstants.ACCOUNT_IDENTIFIER);
    final Optional<GitOpsProviderResponseDTO> getDTO = gitopsProviderService.get(TestConstants.ACCOUNT_IDENTIFIER,
        savedDTO.getOrgIdentifier(), savedDTO.getProjectIdentifier(), savedDTO.getIdentifier());
    match(inputDTO, getDTO.get());
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testUpdate() {
    final GitOpsProviderDTO inputDTO = buildConnectedGitOpsDTO("prefix");
    final GitOpsProviderResponseDTO savedDTO = gitopsProviderService.create(inputDTO, TestConstants.ACCOUNT_IDENTIFIER);
    savedDTO.setDescription("updated_description");
    final GitOpsProviderResponseDTO updatedDTO =
        gitopsProviderService.update(savedDTO, TestConstants.ACCOUNT_IDENTIFIER);
    assertThat(updatedDTO.getDescription()).isEqualTo("updated_description");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testList() {
    final int n = 50;
    final List<GitOpsProviderDTO> toSave =
        IntStream.range(0, n).mapToObj(i -> buildConnectedGitOpsDTO("prefix")).collect(Collectors.toList());
    toSave.forEach(dto -> gitopsProviderService.create(dto, TestConstants.ACCOUNT_IDENTIFIER));
    final Page<GitOpsProviderResponseDTO> listDtos = gitopsProviderService.list(Pageable.unpaged(),
        TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID, TestConstants.PROJECT_ID, null, null);
    assertThat(listDtos).hasSize(n);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testListWithSearchTerm() {
    final int n = 50;
    IntStream.range(0, n)
        .mapToObj(i -> buildConnectedGitOpsDTO("prefix-A-"))
        .collect(Collectors.toList())
        .forEach(dto -> gitopsProviderService.create(dto, TestConstants.ACCOUNT_IDENTIFIER));
    IntStream.range(0, n)
        .mapToObj(i -> buildConnectedGitOpsDTO("prefix-B-"))
        .collect(Collectors.toList())
        .forEach(dto -> gitopsProviderService.create(dto, TestConstants.ACCOUNT_IDENTIFIER));
    IntStream.range(0, n)
        .mapToObj(i -> buildConnectedGitOpsDTO("foo-B-"))
        .collect(Collectors.toList())
        .forEach(dto -> gitopsProviderService.create(dto, TestConstants.ACCOUNT_IDENTIFIER));
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "prefix-B", null))
        .hasSize(n);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "prefix-", null))
        .hasSize(n * 2);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  @Ignore("This test is no more required.")
  public void testListWithSearchTermOnTags() {
    final ImmutableMap<String, String> tagSet1 = ImmutableMap.of("k1", "", "k2", "");
    final ImmutableMap<String, String> tagSet2 = ImmutableMap.of("k2", "");
    final ImmutableMap<String, String> tagSet3 = ImmutableMap.of("k3", "", "k1", "");
    final ImmutableMap<String, String> tagSet4 = ImmutableMap.of("k3", "v3");
    gitopsProviderService.create(buildConnectedGitOpsDTO(tagSet1), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTO(tagSet2), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTO(tagSet3), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTO(tagSet4), TestConstants.ACCOUNT_IDENTIFIER);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "k1", null))
        .hasSize(2);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "k2", null))
        .hasSize(2);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "k3", null))
        .hasSize(1);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "k3:v3", null))
        .hasSize(1);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "foobar", null))
        .hasSize(0);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testListWithSearchTermOnDescription() {
    gitopsProviderService.create(buildConnectedGitOpsDTODesc("desc1"), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTODesc("desc2"), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTODesc("desc3"), TestConstants.ACCOUNT_IDENTIFIER);

    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "desc", null))
        .hasSize(3);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "desc2", null))
        .hasSize(1);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "desc3", null))
        .hasSize(1);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testListWithSearchTermOnName() {
    gitopsProviderService.create(buildConnectedGitOpsDTOName("name1"), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTOName("name2"), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTOName("name3"), TestConstants.ACCOUNT_IDENTIFIER);

    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "name", null))
        .hasSize(3);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "name2", null))
        .hasSize(1);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "name1", null))
        .hasSize(1);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testListWithSearchTermOnUrl() {
    gitopsProviderService.create(buildConnectedGitOpsDTOUrl("https://provider1.com"), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTOUrl("https://provider2.com"), TestConstants.ACCOUNT_IDENTIFIER);
    gitopsProviderService.create(buildConnectedGitOpsDTOUrl("https://provider3.com"), TestConstants.ACCOUNT_IDENTIFIER);

    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "provider", null))
        .hasSize(3);
    assertThat(gitopsProviderService.list(Pageable.unpaged(), TestConstants.ACCOUNT_IDENTIFIER, TestConstants.ORG_ID,
                   TestConstants.PROJECT_ID, "provider2", null))
        .hasSize(1);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testDelete() {
    final GitOpsProviderDTO inputDTO = buildConnectedGitOpsDTO("prefix");
    gitopsProviderService.create(inputDTO, TestConstants.ACCOUNT_IDENTIFIER);
    assertThat(gitopsProviderService
                   .get(TestConstants.ACCOUNT_IDENTIFIER, inputDTO.getOrgIdentifier(), inputDTO.getProjectIdentifier(),
                       inputDTO.getIdentifier())
                   .isPresent())
        .isTrue();
    gitopsProviderService.delete(TestConstants.ACCOUNT_IDENTIFIER, inputDTO.getOrgIdentifier(),
        inputDTO.getProjectIdentifier(), inputDTO.getIdentifier());
    assertThat(gitopsProviderService
                   .get(TestConstants.ACCOUNT_IDENTIFIER, inputDTO.getOrgIdentifier(), inputDTO.getProjectIdentifier(),
                       inputDTO.getIdentifier())
                   .isPresent())
        .isFalse();
  }

  private GitOpsProviderDTO buildConnectedGitOpsDTO(String idPrefix) {
    return GitOpsProviderDTO.builder()
        .name(TestConstants.ARGO_NAME)
        .identifier(idPrefix + generateUuid())
        .orgIdentifier(TestConstants.ORG_ID)
        .projectIdentifier(TestConstants.PROJECT_ID)
        .description(TestConstants.DESCRIPTION)
        .tags(TAGS)
        .infoDTO(ConnectedArgoGitOpsInfoDTO.builder().adapterUrl(TestConstants.ADAPTER_URL).build())
        .build();
  }

  private GitOpsProviderDTO buildConnectedGitOpsDTODesc(String description) {
    return GitOpsProviderDTO.builder()
        .name(TestConstants.ARGO_NAME)
        .identifier(generateUuid())
        .orgIdentifier(TestConstants.ORG_ID)
        .projectIdentifier(TestConstants.PROJECT_ID)
        .description(description)
        .tags(TAGS)
        .infoDTO(ConnectedArgoGitOpsInfoDTO.builder().adapterUrl(TestConstants.ADAPTER_URL).build())
        .build();
  }
  private GitOpsProviderDTO buildConnectedGitOpsDTOName(String name) {
    return GitOpsProviderDTO.builder()
        .name(name)
        .identifier(generateUuid())
        .orgIdentifier(TestConstants.ORG_ID)
        .projectIdentifier(TestConstants.PROJECT_ID)
        .description(TestConstants.DESCRIPTION)
        .tags(TAGS)
        .infoDTO(ConnectedArgoGitOpsInfoDTO.builder().adapterUrl(TestConstants.ADAPTER_URL).build())
        .build();
  }

  private GitOpsProviderDTO buildConnectedGitOpsDTO(Map<String, String> tags) {
    return GitOpsProviderDTO.builder()
        .name(TestConstants.ARGO_NAME)
        .identifier(generateUuid())
        .orgIdentifier(TestConstants.ORG_ID)
        .projectIdentifier(TestConstants.PROJECT_ID)
        .description(TestConstants.DESCRIPTION)
        .tags(tags)
        .infoDTO(ConnectedArgoGitOpsInfoDTO.builder().adapterUrl(TestConstants.ADAPTER_URL).build())
        .build();
  }

  private GitOpsProviderDTO buildConnectedGitOpsDTOUrl(String url) {
    return GitOpsProviderDTO.builder()
        .name(TestConstants.ARGO_NAME)
        .identifier(generateUuid())
        .orgIdentifier(TestConstants.ORG_ID)
        .projectIdentifier(TestConstants.PROJECT_ID)
        .description(TestConstants.DESCRIPTION)
        .infoDTO(ConnectedArgoGitOpsInfoDTO.builder().adapterUrl(url).build())
        .build();
  }

  private void match(GitOpsProviderDTO req, GitOpsProviderResponseDTO response) {
    GitOpsProviderDTO resp = response;
    assertThat(req.getName()).isEqualTo(resp.getName());
    assertThat(req.getIdentifier()).isEqualTo(resp.getIdentifier());
    assertThat(req.getOrgIdentifier()).isEqualTo(resp.getOrgIdentifier());
    assertThat(req.getProjectIdentifier()).isEqualTo(resp.getProjectIdentifier());
    assertThat(req.getTags()).isEqualTo(resp.getTags());
    assertThat(req.getInfoDTO()).isEqualTo(resp.getInfoDTO());
    assertThat(req.getName()).isEqualTo(resp.getName());
  }
}
