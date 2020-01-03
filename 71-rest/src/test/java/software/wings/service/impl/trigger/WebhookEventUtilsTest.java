package software.wings.service.impl.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.trigger.WebhookSource.BITBUCKET;
import static software.wings.beans.trigger.WebhookSource.GITHUB;
import static software.wings.beans.trigger.WebhookSource.GITLAB;
import static software.wings.service.impl.trigger.WebhookEventUtils.X_BIT_BUCKET_EVENT;
import static software.wings.service.impl.trigger.WebhookEventUtils.X_GIT_HUB_EVENT;
import static software.wings.service.impl.trigger.WebhookEventUtils.X_GIT_LAB_EVENT;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.trigger.WebhookSource.BitBucketEventType;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;
import software.wings.beans.trigger.WebhookSource.GitLabEventType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;

public class WebhookEventUtilsTest extends WingsBaseTest {
  @Inject @InjectMocks private WebhookEventUtils webhookEventUtils;
  static final String GH_PUSH_REQ_FILE = "software/wings/service/impl/webhook/github_push_request.json";
  static final String GH_PULL_REQ_FILE = "software/wings/service/impl/webhook/github_pull_request.json";
  static final String GITLAB_PUSH_REQ_FILE = "software/wings/service/impl/webhook/gitlab_push_request.json";
  static final String GITLAB_PULL_REQ_FILE = "software/wings/service/impl/webhook/gitlab_pullrequest.json";
  static final String BITBUCKET_PUSH_REQ_FILE = "software/wings/service/impl/webhook/bitbucket_push_request.json";
  static final String BITBUCKET_PULL_REQ_FILE =
      "software/wings/service/impl/webhook/bitbucket_pull_request_created.json";
  @Mock HttpHeaders httpHeaders;

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldObtainWebhookSource() {
    when(httpHeaders.getHeaderString(X_GIT_HUB_EVENT))
        .thenReturn(GitHubEventType.PUSH.name())
        .thenReturn(null)
        .thenReturn(null);
    assertThat(webhookEventUtils.obtainWebhookSource(httpHeaders)).isEqualTo(GITHUB);
    when(httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT)).thenReturn(BitBucketEventType.PUSH.name()).thenReturn(null);
    assertThat(webhookEventUtils.obtainWebhookSource(httpHeaders)).isEqualTo(BITBUCKET);
    when(httpHeaders.getHeaderString(X_GIT_LAB_EVENT)).thenReturn(GitLabEventType.PUSH.name());
    assertThat(webhookEventUtils.obtainWebhookSource(httpHeaders)).isEqualTo(GITLAB);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldObtainGHPushBranchAndCommitId() throws IOException {
    when(httpHeaders.getHeaderString(X_GIT_HUB_EVENT)).thenReturn(GitHubEventType.PUSH.getValue());
    Map<String, Object> payload = obtainPayload(GH_PUSH_REQ_FILE);
    assertThat(webhookEventUtils.obtainBranchName(GITHUB, httpHeaders, payload)).isEqualTo("master");
    assertThat(webhookEventUtils.obtainCommitId(GITHUB, httpHeaders, payload))
        .isEqualTo("4ebc6e9e489979a29ca17b8da0c29d9f6803a5b9");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldObtainGHPullRequestBranchName() throws IOException {
    when(httpHeaders.getHeaderString(X_GIT_HUB_EVENT)).thenReturn(GitHubEventType.PULL_REQUEST.getValue());
    Map<String, Object> payload = obtainPayload(GH_PULL_REQ_FILE);
    assertThat(webhookEventUtils.obtainBranchName(GITHUB, httpHeaders, payload)).isEqualTo("puneet/istio-rollback");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldObtainBitBucketPushBranchAndCommitId() throws IOException {
    when(httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT)).thenReturn(BitBucketEventType.PUSH.getValue());
    Map<String, Object> payload = obtainPayload(BITBUCKET_PUSH_REQ_FILE);
    assertThat(webhookEventUtils.obtainBranchName(BITBUCKET, httpHeaders, payload)).isEqualTo("master");
    assertThat(webhookEventUtils.obtainCommitId(BITBUCKET, httpHeaders, payload))
        .isEqualTo("b603ef9afef725673e908cef5c377f79aa532de0");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldObtainBitBucketPullBranchName() throws IOException {
    when(httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT))
        .thenReturn(BitBucketEventType.PULL_REQUEST_CREATED.getValue());
    Map<String, Object> payload = obtainPayload(BITBUCKET_PULL_REQ_FILE);
    assertThat(webhookEventUtils.obtainBranchName(BITBUCKET, httpHeaders, payload)).isEqualTo("harshBranch");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldObtainGitLabBranchAndCommitId() throws IOException {
    when(httpHeaders.getHeaderString(X_GIT_LAB_EVENT)).thenReturn(GitLabEventType.PUSH.getValue());
    Map<String, Object> payload = obtainPayload(GITLAB_PUSH_REQ_FILE);
    assertThat(webhookEventUtils.obtainBranchName(GITLAB, httpHeaders, payload)).isEqualTo("master");
    assertThat(webhookEventUtils.obtainCommitId(GITLAB, httpHeaders, payload))
        .isEqualTo("da1560886d4f094c3e6c9ef40349f7d38b5d27d7");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldObtainGitLabBranchNameForPullRequest() throws IOException {
    when(httpHeaders.getHeaderString(X_GIT_LAB_EVENT)).thenReturn(GitLabEventType.PULL_REQUEST.getValue());
    Map<String, Object> payload = obtainPayload(GITLAB_PULL_REQ_FILE);
    assertThat(webhookEventUtils.obtainBranchName(GITLAB, httpHeaders, payload)).isEqualTo("harshBranch");
  }

  private Map<String, Object> obtainPayload(String filePath) throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();

    File file = new File(classLoader.getResource(filePath).getFile());
    return JsonUtils.asObject(
        FileUtils.readFileToString(file, Charset.defaultCharset()), new TypeReference<Map<String, Object>>() {});
  }
}