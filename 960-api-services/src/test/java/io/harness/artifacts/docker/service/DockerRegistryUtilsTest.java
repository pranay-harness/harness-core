/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.docker.service;

import static io.harness.artifacts.docker.beans.DockerImageManifestResponse.DockerImageManifestHistoryElement;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;
import static io.harness.rule.OwnerRule.HARSH;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.docker.DockerRegistryRestClient;
import io.harness.artifacts.docker.beans.DockerImageManifestResponse;
import io.harness.artifacts.docker.beans.DockerInternalConfig;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collections;
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

@OwnedBy(HarnessTeam.CDC)
public class DockerRegistryUtilsTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private DockerRegistryRestClient dockerRegistryRestClient;
  @InjectMocks DockerRegistryUtils dockerRegistryUtils = new DockerRegistryUtils();
  private static final String AUTH_HEADER = "AUTH_HEADER";
  private static final String IMAGE_NAME = "IMAGE_NAME";
  private static final DockerInternalConfig dockerConfig =
      DockerInternalConfig.builder().dockerRegistryUrl("https://registry.hub.docker.com/v2/").build();

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldNotGetLabelsIfEmptyTags() {
    List<Map<String, String>> labelsMap =
        dockerRegistryUtils.getLabels(dockerConfig, dockerRegistryRestClient, null, AUTH_HEADER, IMAGE_NAME, asList());
    assertThat(labelsMap).isEmpty();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGetLabelsIfNonEmptyTags() throws IOException {
    Call<DockerImageManifestResponse> requestCall = mock(Call.class);
    when(requestCall.execute()).thenReturn(Response.success(getDockerImageManifestResponse(null)));
    when(dockerRegistryRestClient.getImageManifest(any(), any(), any())).thenReturn(requestCall);

    List<Map<String, String>> labelsMap = dockerRegistryUtils.getLabels(
        dockerConfig, dockerRegistryRestClient, null, AUTH_HEADER, IMAGE_NAME, asList("abc", "abc1"));
    assertThat(labelsMap).isNotEmpty();
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void shouldGetLabelsIfNonEmptyTagsWithValidLabels() throws IOException {
    Call<DockerImageManifestResponse> requestCall = mock(Call.class);
    when(requestCall.execute())
        .thenReturn(Response.success(getDockerImageManifestResponse(
            "{\"architecture\":\"amd64\",\"config\":{\"Labels\":{\"maintainer\":\"NGINX Docker Maintainers <docker-maint@nginx.com>\",\"please\":\"work\"}}}")))
        .thenReturn(Response.success(getDockerImageManifestResponse(
            "{\"architecture\":\"amd64\",\"container_config\":{\"Labels\":{\"appname\":\"python-hello\",\"author\":\"deepakputhraya\"}},\"config\":{\"Labels\":{\"appname\":\"hello-world\",\"author\":\"deepakputhraya\",\"version\":\"4.0\"}}}")))
        .thenReturn(Response.success(getDockerImageManifestResponse(
            "{\"architecture\":\"amd64\",\"container_config\":{\"Labels\":{\"maintainer\":\"docker-maint@nginx.com\",\"please\":\"work\"}}}")));
    when(dockerRegistryRestClient.getImageManifest(any(), any(), any())).thenReturn(requestCall);

    List<Map<String, String>> labelsMap = dockerRegistryUtils.getLabels(
        dockerConfig, dockerRegistryRestClient, null, AUTH_HEADER, IMAGE_NAME, asList("abc", "abc1", "abc3"));
    assertThat(labelsMap).isNotEmpty();
    assertThat(labelsMap).hasSize(3);

    assertThat(labelsMap.get(0)).hasSize(2);
    assertThat(labelsMap.get(0))
        .isEqualTo(ImmutableMap.<String, String>builder()
                       .put("please", "work")
                       .put("maintainer", "NGINX Docker Maintainers <docker-maint@nginx.com>")
                       .build());

    assertThat(labelsMap.get(1)).hasSize(3);
    assertThat(labelsMap.get(1))
        .isEqualTo(ImmutableMap.<String, String>builder()
                       .put("appname", "python-hello")
                       .put("author", "deepakputhraya")
                       .put("version", "4.0")
                       .build());

    assertThat(labelsMap.get(2)).hasSize(2);
    assertThat(labelsMap.get(2))
        .isEqualTo(ImmutableMap.<String, String>builder()
                       .put("please", "work")
                       .put("maintainer", "docker-maint@nginx.com")
                       .build());
  }

  private DockerImageManifestResponse getDockerImageManifestResponse(String v1Compatibility) {
    DockerImageManifestResponse dockerImageManifestResponse = new DockerImageManifestResponse();
    dockerImageManifestResponse.setName("abc");
    DockerImageManifestHistoryElement history = new DockerImageManifestHistoryElement();
    history.setV1Compatibility(v1Compatibility);
    dockerImageManifestResponse.setHistory(Collections.singletonList(history));
    return dockerImageManifestResponse;
  }
}
