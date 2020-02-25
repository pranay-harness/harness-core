package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Lists;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import software.wings.WingsBaseTest;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.yaml.GitSyncService;
import software.wings.utils.ResourceTestRule;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * @author vardanb
 */
public class GitSyncResourceTest extends WingsBaseTest {
  private static final GitSyncService GIT_SYNC_SERVICE = mock(GitSyncService.class);

  @Captor private ArgumentCaptor<PageRequest<GitSyncError>> pageRequestArgumentCaptor;

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new GitSyncResource(GIT_SYNC_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  private static final GitSyncError GIT_SYNC_ERROR =
      GitSyncError.builder().accountId(ACCOUNT_ID).yamlFilePath("yamlFilePath").gitCommitId("gitCommitId").build();

  /**
   * Should list git sync errors.
   */
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void shouldListErrors() {
    PageResponse<GitSyncError> pageResponse = aPageResponse().withResponse(Lists.newArrayList(GIT_SYNC_ERROR)).build();
    when(GIT_SYNC_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);

    RestResponse<PageResponse<GitSyncError>> restResponse =
        RESOURCES.client()
            .target(format("/git-sync/errors?accountId=%s", ACCOUNT_ID))
            .request()
            .get(new GenericType<RestResponse<PageResponse<GitSyncError>>>() {});

    log().info(JsonUtils.asJson(restResponse));
    verify(GIT_SYNC_SERVICE).list(pageRequestArgumentCaptor.capture());
    assertThat(pageRequestArgumentCaptor.getValue()).isNotNull();
    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", pageResponse);
  }

  /**
   * Should update git sync error status
   */
  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category(UnitTests.class)
  public void shouldDiscardErrors() {
    RestResponse restResponse = RESOURCES.client()
                                    .target(format("/git-sync/discard?accountId=%s", ACCOUNT_ID))
                                    .request()
                                    .post(entity(Arrays.asList(GitSyncError.builder()
                                                                   .accountId(ACCOUNT_ID)
                                                                   .yamlFilePath("yamlFilePath")
                                                                   .gitCommitId("gitCommitId")
                                                                   .build()),
                                              MediaType.APPLICATION_JSON),
                                        new GenericType<RestResponse<List<GitSyncError>>>() {});

    assertThat(restResponse).isNotNull();
  }
}