package io.harness.connector.mappers.gitconnectormapper;

import static io.harness.delegate.beans.connector.gitconnector.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.gitconnector.GitConnectionType.ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.gitconnector.GitConfigSummaryDTO;
import io.harness.connector.entities.embedded.gitconnector.GitConfig;
import io.harness.connector.entities.embedded.gitconnector.GitUserNamePasswordAuthentication;
import io.harness.delegate.beans.connector.gitconnector.CustomCommitAttributes;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class GitConfigSummaryMapperTest extends CategoryTest {
  @InjectMocks GitConfigSummaryMapper gitConfigSummaryMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void createGitConfigSummaryDTOTest() {
    String url = "url";
    String userName = "userName";
    String passwordReference = "password";
    CustomCommitAttributes customCommitAttributes = CustomCommitAttributes.builder()
                                                        .authorEmail("author")
                                                        .authorName("authorName")
                                                        .commitMessage("commitMessage")
                                                        .build();
    GitUserNamePasswordAuthentication gitUserNamePasswordAuthentication =
        GitUserNamePasswordAuthentication.builder().userName(userName).passwordReference(passwordReference).build();
    GitConfig gitConfig = GitConfig.builder()
                              .supportsGitSync(true)
                              .authType(HTTP)
                              .url(url)
                              .connectionType(ACCOUNT)
                              .customCommitAttributes(customCommitAttributes)
                              .authenticationDetails(gitUserNamePasswordAuthentication)
                              .build();
    GitConfigSummaryDTO gitConfigSummaryDTO = gitConfigSummaryMapper.createGitConfigSummaryDTO(gitConfig);
    assertThat(gitConfigSummaryDTO).isNotNull();
    assertThat(gitConfigSummaryDTO.getUrl()).isEqualTo(url);
  }
}