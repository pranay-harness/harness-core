package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import io.harness.CategoryTest;
import io.harness.beans.PRWebhookEvent;
import io.harness.beans.Repository;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
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
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class GitWebhookTriggerRepoFilterTest extends CategoryTest {
  @Mock private NGTriggerService ngTriggerService;
  @Inject @InjectMocks private GitWebhookTriggerRepoFilter filter;
  private static List<TriggerDetails> triggerDetailsList;
  private static List<ConnectorResponseDTO> connectors;
  private static Repository repository1 = Repository.builder()
                                              .httpURL("https://github.com/owner1/repo1.git")
                                              .sshURL("git@github.com:owner1/repo1")
                                              .link("https://github.com/owner1/repo1")
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

    triggerDetailsList = Arrays.asList(details1, details2, details3);
    connectors = Arrays.asList(connectorResponseDTO1, connectorResponseDTO2, connectorResponseDTO3);
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
            .projectFqn("p")
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
            .projectFqn("p")
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
            .projectFqn("p")
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
}
