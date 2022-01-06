/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PRWebhookEvent;
import io.harness.beans.Repository;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.ngtriggers.NgTriggersTestHelper;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.GitMetadata;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.eventmapper.TriggerGitConnectorWrapper;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.utils.GitProviderDataObtainmentManager;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class GitWebhookTriggerRepoFilterTest extends CategoryTest {
  @Mock private NGTriggerService ngTriggerService;
  @Mock private GitProviderDataObtainmentManager dataObtainmentManager;
  @Inject @InjectMocks private GitWebhookTriggerRepoFilter filter;
  private static List<TriggerDetails> triggerDetailsList;
  private static List<ConnectorResponseDTO> connectors;
  private static Repository repository1 = Repository.builder()
                                              .httpURL("https://github.com/owner1/repo1.git")
                                              .sshURL("git@github.com:owner1/repo1.git")
                                              .link("https://github.com/owner1/repo1/b")
                                              .build();

  private static Repository repository2 = Repository.builder()
                                              .httpURL("https://github.com/owner1/repo2.git")
                                              .sshURL("git@github.com:owner1/repo2")
                                              .link("https://github.com/owner1/repo2")
                                              .build();

  private static Repository repository3 = Repository.builder()
                                              .httpURL("https://github.com/owner1/repo3.git")
                                              .sshURL("git@github.com:owner1/repo3")
                                              .link("https://github.com/owner1/repo3")
                                              .build();

  static {
    TriggerDetails details1 =
        TriggerDetails.builder()
            .ngTriggerEntity(
                NGTriggerEntity.builder()
                    .accountId("acc")
                    .orgIdentifier("org")
                    .projectIdentifier("proj")
                    .metadata(NGTriggerMetadata.builder()
                                  .webhook(WebhookMetadata.builder()
                                               .type("GITHUB")
                                               .git(GitMetadata.builder().connectorIdentifier("account.con1").build())
                                               .build())
                                  .build())
                    .build())
            .build();
    ConnectorResponseDTO connectorResponseDTO1 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder()
                           .connectorConfig(GithubConnectorDTO.builder()
                                                .connectionType(GitConnectionType.REPO)
                                                .url("https://github.com/owner1/repo1")
                                                .build())
                           .identifier("con1")
                           .build())
            .build();

    TriggerDetails details2 =
        TriggerDetails.builder()
            .ngTriggerEntity(
                NGTriggerEntity.builder()
                    .accountId("acc")
                    .orgIdentifier("org")
                    .projectIdentifier("proj")
                    .metadata(
                        NGTriggerMetadata.builder()
                            .webhook(
                                WebhookMetadata.builder()
                                    .type("GITHUB")
                                    .git(
                                        GitMetadata.builder().repoName("repo2").connectorIdentifier("org.con1").build())
                                    .build())
                            .build())
                    .build())
            .build();
    ConnectorResponseDTO connectorResponseDTO2 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder()
                           .connectorConfig(GithubConnectorDTO.builder()
                                                .connectionType(GitConnectionType.ACCOUNT)
                                                .url("https://github.com/owner1")
                                                .build())
                           .orgIdentifier("org")
                           .identifier("con1")
                           .build())
            .build();

    TriggerDetails details3 =
        TriggerDetails.builder()
            .ngTriggerEntity(
                NGTriggerEntity.builder()
                    .accountId("acc")
                    .orgIdentifier("org")
                    .projectIdentifier("proj")
                    .metadata(
                        NGTriggerMetadata.builder()
                            .webhook(
                                WebhookMetadata.builder()
                                    .type("GITHUB")
                                    .git(GitMetadata.builder().repoName("repo3").connectorIdentifier("con1").build())
                                    .build())
                            .build())
                    .build())
            .build();
    ConnectorResponseDTO connectorResponseDTO3 =
        ConnectorResponseDTO.builder()
            .connector(ConnectorInfoDTO.builder()
                           .connectorConfig(GithubConnectorDTO.builder()
                                                .connectionType(GitConnectionType.ACCOUNT)
                                                .url("https://github.com/owner1")
                                                .build())
                           .orgIdentifier("org")
                           .projectIdentifier("proj")
                           .identifier("con1")
                           .build())
            .build();

    triggerDetailsList = asList(details1, details2, details3);
    connectors = asList(connectorResponseDTO1, connectorResponseDTO2, connectorResponseDTO3);
  }

  @Before
  public void setUp() throws IOException {
    initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void applyRepoUrlFilterTest() {
    doReturn(connectors).when(ngTriggerService).fetchConnectorsByFQN(eq("acc"), anyList());

    FilterRequestData filterRequestData =
        FilterRequestData.builder()
            .accountId("p")
            .webhookPayloadData(
                WebhookPayloadData.builder()
                    .originalEvent(TriggerWebhookEvent.builder().accountId("acc").sourceRepoType("GITHUB").build())
                    .webhookEvent(PRWebhookEvent.builder().repository(repository1).build())
                    .repository(repository1)
                    .build())
            .details(triggerDetailsList)
            .build();
    WebhookEventMappingResponse webhookEventMappingResponse = filter.applyFilter(filterRequestData);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    List<TriggerDetails> triggerDetails = webhookEventMappingResponse.getTriggers();
    assertThat(triggerDetails.size()).isEqualTo(1);
    assertThat(triggerDetails.get(0)).isEqualTo(triggerDetailsList.get(0));

    filterRequestData =
        FilterRequestData.builder()
            .accountId("p")
            .webhookPayloadData(
                WebhookPayloadData.builder()
                    .originalEvent(TriggerWebhookEvent.builder().accountId("acc").sourceRepoType("GITHUB").build())
                    .webhookEvent(PRWebhookEvent.builder().repository(repository2).build())
                    .repository(repository2)
                    .build())
            .details(triggerDetailsList)
            .build();
    webhookEventMappingResponse = filter.applyFilter(filterRequestData);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    triggerDetails = webhookEventMappingResponse.getTriggers();
    assertThat(triggerDetails.size()).isEqualTo(1);
    assertThat(triggerDetails.get(0)).isEqualTo(triggerDetailsList.get(1));

    filterRequestData =
        FilterRequestData.builder()
            .accountId("p")
            .webhookPayloadData(
                WebhookPayloadData.builder()
                    .originalEvent(TriggerWebhookEvent.builder().accountId("acc").sourceRepoType("GITHUB").build())
                    .webhookEvent(PRWebhookEvent.builder().repository(repository3).build())
                    .repository(repository3)
                    .build())
            .details(triggerDetailsList)
            .build();
    webhookEventMappingResponse = filter.applyFilter(filterRequestData);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    triggerDetails = webhookEventMappingResponse.getTriggers();
    assertThat(triggerDetails.size()).isEqualTo(1);
    assertThat(triggerDetails.get(0)).isEqualTo(triggerDetailsList.get(2));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void prepareTriggerConnectorWrapperListTest() {
    doReturn(connectors).when(ngTriggerService).fetchConnectorsByFQN(eq("acc"), anyList());
    List<TriggerGitConnectorWrapper> connectorWrappers =
        filter.prepareTriggerConnectorWrapperList("acc", triggerDetailsList);

    assertThat(connectorWrappers.size()).isEqualTo(3);

    TriggerGitConnectorWrapper connector = getConnectorWrapperFromList(connectorWrappers, "acc/con1");
    assertThat(connector.getTriggers().size()).isEqualTo(1);
    assertThat(connector.getTriggers().get(0)).isEqualTo(triggerDetailsList.get(0));
    assertThat(connector.getConnectorConfigDTO()).isEqualTo(connectors.get(0).getConnector().getConnectorConfig());

    connector = getConnectorWrapperFromList(connectorWrappers, "acc/org/con1");
    assertThat(connector.getTriggers().size()).isEqualTo(1);
    assertThat(connector.getTriggers().get(0)).isEqualTo(triggerDetailsList.get(1));
    assertThat(connector.getConnectorConfigDTO()).isEqualTo(connectors.get(1).getConnector().getConnectorConfig());

    connector = getConnectorWrapperFromList(connectorWrappers, "acc/org/proj/con1");
    assertThat(connector.getTriggers().size()).isEqualTo(1);
    assertThat(connector.getTriggers().get(0)).isEqualTo(triggerDetailsList.get(2));
    assertThat(connector.getConnectorConfigDTO()).isEqualTo(connectors.get(2).getConnector().getConnectorConfig());
  }

  private TriggerGitConnectorWrapper getConnectorWrapperFromList(
      List<TriggerGitConnectorWrapper> connectorWrappers, String fqn) {
    return connectorWrappers.stream()
        .filter(connector -> connector.getConnectorFQN().equals(fqn))
        .findFirst()
        .orElse(null);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldFilterAWSCodecommitTrigger() {
    Repository repository = Repository.builder().id("arn:aws:codecommit:eu-central-1:44864EXAMPLE:test").build();
    List<ConnectorResponseDTO> connectors = asList(NgTriggersTestHelper.getAwsCodeCommitRegionConnectorResponsesDTO(),
        NgTriggersTestHelper.getAwsCodeCommitRepoConnectorResponsesDTO(),
        NgTriggersTestHelper.getAwsCodeCommitRepoConnectorResponsesDTO2());

    doReturn(connectors).when(ngTriggerService).fetchConnectorsByFQN(eq("acc"), anyList());

    List<TriggerDetails> triggerDetails = asList(NgTriggersTestHelper.getAwsRepoTriggerDetails(),
        NgTriggersTestHelper.getAwsRegionTriggerDetails(), NgTriggersTestHelper.getAwsRepoTriggerDetails2());

    FilterRequestData filterRequestData =
        FilterRequestData.builder()
            .accountId("acc")
            .webhookPayloadData(WebhookPayloadData.builder()
                                    .originalEvent(TriggerWebhookEvent.builder()
                                                       .accountId("acc")
                                                       .orgIdentifier("org")
                                                       .projectIdentifier("proj")
                                                       .sourceRepoType("AWS_CODECOMMIT")
                                                       .build())
                                    .webhookEvent(PRWebhookEvent.builder().repository(repository).build())
                                    .repository(repository)
                                    .build())
            .details(triggerDetails)
            .build();
    WebhookEventMappingResponse webhookEventMappingResponse = filter.applyFilter(filterRequestData);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    triggerDetails = webhookEventMappingResponse.getTriggers();
    assertThat(triggerDetails.size()).isEqualTo(2);
    assertThat(triggerDetails)
        .containsOnly(
            NgTriggersTestHelper.getAwsRepoTriggerDetails(), NgTriggersTestHelper.getAwsRegionTriggerDetails());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetUrls() {
    HashSet<String> urls = filter.getUrls(repository1, "Github");
    assertThat(urls).containsExactlyInAnyOrder("https://github.com/owner1/repo1.git", "https://github.com/owner1/repo1",
        "git@github.com:owner1/repo1.git", "git@github.com:owner1/repo1", "https://github.com/owner1/repo1/b");
  }
}
