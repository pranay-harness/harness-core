/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.UTSAV;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceBuilder;
import io.kubernetes.client.util.ClientBuilder;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NamespaceFetcherTest extends CategoryTest {
  private SharedInformerFactory sharedInformerFactory;
  private NamespaceFetcher namespaceFetcher;
  private V1Namespace sampleNamespace;

  private static final String NAME = "harness-delegate";
  private static final Map<String, String> LABELS = ImmutableMap.of("k1", "v1", "k2", "v2");

  @Rule public WireMockRule wireMockRule = new WireMockRule(65226);
  private static final String URL_REGEX_SUFFIX = "(\\?(.*))?";
  private static final String GET_NAMESPACES_URL = "^/api/v1/namespaces/" + NAME + URL_REGEX_SUFFIX;

  @Before
  public void setUp() throws Exception {
    sharedInformerFactory = new SharedInformerFactory();

    namespaceFetcher = new NamespaceFetcher(
        new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build(), sharedInformerFactory);

    sampleNamespace =
        new V1NamespaceBuilder().withNewMetadata().withName(NAME).withLabels(LABELS).endMetadata().build();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldCreateConstructor() {
    assertThat(namespaceFetcher).isNotNull();
    assertThat(sharedInformerFactory.getExistingSharedIndexInformer(V1Namespace.class)).isNotNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  @Ignore("TODO: fix this test for the new version of the com.github.tomakehurst.wiremock.client")
  public void shouldThrowExceptionIfNamespaceNotFound() {
    stubFor(get(urlMatching(GET_NAMESPACES_URL)).willReturn(aResponse()));
    assertThatThrownBy(() -> namespaceFetcher.getNamespaceByKey(NAME))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Not Found");
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetByKeyFromCoreV1Api() throws Exception {
    stubFor(get(urlMatching(GET_NAMESPACES_URL))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new Gson().toJson(sampleNamespace))));

    V1Namespace namespaceFetched = namespaceFetcher.getNamespaceByKey(NAME);
    verify(1, getRequestedFor(urlMatching(GET_NAMESPACES_URL)));
    assertThat(namespaceFetched.getMetadata().getLabels()).isEqualTo(LABELS);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetByKeyFromStore() throws Exception {
    sharedInformerFactory.getExistingSharedIndexInformer(V1Namespace.class).getIndexer().add(sampleNamespace);

    V1Namespace namespaceFetched = namespaceFetcher.getNamespaceByKey(NAME);
    assertThat(namespaceFetched.getMetadata().getLabels()).isEqualTo(LABELS);
    verify(0, getRequestedFor(urlMatching(GET_NAMESPACES_URL)));
  }
}
