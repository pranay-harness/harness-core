/*
 * Copyright 2020 Harness Inc. All rights reserved.
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.Gson;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimBuilder;
import io.kubernetes.client.util.ClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PVCFetcherTest extends CategoryTest {
  private SharedInformerFactory sharedInformerFactory;
  private PVCFetcher pvcFetcher;
  private V1PersistentVolumeClaim testPVC;
  private static final String NAMESPACE = "harness";
  private static final String CLAIM_NAME = "mongo-data";

  @Rule public WireMockRule wireMockRule = new WireMockRule(65224);
  private static final String URL_REGEX_SUFFIX = "(\\?(.*))?";
  private static final String GET_NAMESPACED_PVC_URL =
      "^/api/v1/namespaces/" + NAMESPACE + "/persistentvolumeclaims/" + CLAIM_NAME + URL_REGEX_SUFFIX;

  @Before
  public void setUp() throws Exception {
    sharedInformerFactory = new SharedInformerFactory();

    pvcFetcher = new PVCFetcher(
        new ClientBuilder().setBasePath("http://localhost:" + wireMockRule.port()).build(), sharedInformerFactory);

    testPVC = new V1PersistentVolumeClaimBuilder()
                  .withNewMetadata()
                  .withName(CLAIM_NAME)
                  .withNamespace(NAMESPACE)
                  .endMetadata()
                  .withNewSpec()
                  .withNewResources()
                  .addToRequests("storage", new Quantity("75Gi"))
                  .endResources()
                  .endSpec()
                  .build();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldBuildConstructor() {
    assertThat(pvcFetcher).isNotNull();
    assertThat(sharedInformerFactory.getExistingSharedIndexInformer(V1PersistentVolumeClaim.class)).isNotNull();
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetPvcByKeyWhenStoreIsEmpty() throws ApiException {
    stubFor(get(urlMatching(GET_NAMESPACED_PVC_URL))
                .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody(new Gson().toJson(testPVC))));

    V1PersistentVolumeClaim fetchedPvc = pvcFetcher.getPvcByKey(NAMESPACE, CLAIM_NAME);

    verify(1, getRequestedFor(urlPathMatching(GET_NAMESPACED_PVC_URL)));
    assertThat(fetchedPvc).isNotNull().isEqualTo(testPVC);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testGetPvcByKeyFromStore() throws ApiException {
    sharedInformerFactory.getExistingSharedIndexInformer(V1PersistentVolumeClaim.class).getIndexer().add(testPVC);

    V1PersistentVolumeClaim fetchedPvc = pvcFetcher.getPvcByKey(NAMESPACE, CLAIM_NAME);

    verify(0, getRequestedFor(urlPathMatching(GET_NAMESPACED_PVC_URL)));
    assertThat(fetchedPvc).isNotNull().isEqualTo(testPVC);
  }
}
