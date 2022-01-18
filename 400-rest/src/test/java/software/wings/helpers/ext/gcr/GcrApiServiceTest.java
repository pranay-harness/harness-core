/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.gcr;

import static io.harness.rule.OwnerRule.DEEPAK_PUTHRAYA;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.gcr.beans.GcrInternalConfig;
import io.harness.artifacts.gcr.service.GcrApiServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.mappers.artifact.GcrConfigToInternalMapper;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcrApiServiceTest extends WingsBaseTest {
  GcrApiServiceImpl gcrService = spy(new GcrApiServiceImpl());
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(
      WireMockConfiguration.wireMockConfig().usingFilesUnderDirectory("400-rest/src/test/resources").port(0));
  private String url;
  String basicAuthHeader = "auth";
  GcrInternalConfig gcpInternalConfig;

  @Before
  public void setUp() {
    url = "localhost:" + wireMockRule.port();
    gcpInternalConfig = GcrConfigToInternalMapper.toGcpInternalConfig(url, basicAuthHeader);
    wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/v2/someImage/tags/list"))
                             .withHeader("Authorization", equalTo("auth"))
                             .willReturn(aResponse().withStatus(200).withBody(
                                 "{\"name\":\"someImage\",\"tags\":[\"v1\",\"v2\",\"latest\"]}")));
    when(gcrService.getUrl(anyString())).thenReturn("http://" + url);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void shouldGetBuilds() {
    List<BuildDetailsInternal> actual = gcrService.getBuilds(gcpInternalConfig, "someImage", 100);
    assertThat(actual).hasSize(3);
    assertThat(actual.stream().map(BuildDetailsInternal::getNumber).collect(Collectors.toList()))
        .isEqualTo(Lists.newArrayList("latest", "v1", "v2"));

    gcrService.getBuilds(GcrConfigToInternalMapper.toGcpInternalConfig(url, basicAuthHeader), "someImage", 100);
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testVerifyImageName() {
    assertThat(gcrService.verifyImageName(gcpInternalConfig, "someImage")).isTrue();
  }

  @Test
  @Owner(developers = DEEPAK_PUTHRAYA)
  @Category(UnitTests.class)
  public void testValidateCredentials() {
    assertThat(gcrService.validateCredentials(gcpInternalConfig, "someImage")).isTrue();
  }
}
