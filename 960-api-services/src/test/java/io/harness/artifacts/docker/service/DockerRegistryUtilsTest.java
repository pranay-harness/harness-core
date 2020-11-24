package io.harness.artifacts.docker.service;

import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.HARSH;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.artifacts.docker.DockerRegistryRestClient;
import io.harness.artifacts.docker.beans.DockerImageManifestResponse;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import retrofit2.Response;

public class DockerRegistryUtilsTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private DockerRegistryRestClient dockerRegistryRestClient;
  @InjectMocks DockerRegistryUtils dockerRegistryUtils = new DockerRegistryUtils();
  private static final String AUTH_HEADER = "AUTH_HEADER";
  private static final String IMAGE_NAME = "IMAGE_NAME";

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotGetLabelsIfEmptyTags() {
    List<Map<String, String>> labelsMap =
        dockerRegistryUtils.getLabels(dockerRegistryRestClient, null, AUTH_HEADER, IMAGE_NAME, asList());
    assertThat(labelsMap).isEmpty();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGetLabelsIfNonEmptyTags() throws IOException {
    Call<DockerImageManifestResponse> requestCall = mock(Call.class);
    DockerImageManifestResponse dockerImageManifestResponse = new DockerImageManifestResponse();
    dockerImageManifestResponse.setName("abc");
    Response<DockerImageManifestResponse> response = Response.success(dockerImageManifestResponse);
    when(requestCall.execute()).thenReturn(response);
    when(dockerRegistryRestClient.getImageManifest(any(), any(), any())).thenReturn(requestCall);

    List<Map<String, String>> labelsMap =
        dockerRegistryUtils.getLabels(dockerRegistryRestClient, null, AUTH_HEADER, IMAGE_NAME, asList("abc", "abc1"));
    assertThat(labelsMap).isNotEmpty();
  }
}
