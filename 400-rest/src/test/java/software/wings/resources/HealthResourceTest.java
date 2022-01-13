/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.configuration.ConfigurationType;
import io.harness.mongo.MongoConfig;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.security.AsymmetricEncryptor;

import software.wings.app.MainConfiguration;
import software.wings.exception.WingsExceptionMapper;
import software.wings.search.framework.ElasticsearchConfig;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HealthResourceTest extends CategoryTest {
  public static final MainConfiguration configuration = mock(MainConfiguration.class);
  public static final AsymmetricEncryptor asymmetricEncryptor = mock(AsymmetricEncryptor.class);
  private static final String HEALTH_RESOURCE_URL = "/health/configuration?configurationType=";

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder()
          .instance(new HealthResource(configuration, asymmetricEncryptor, null))
          .type(WingsExceptionMapper.class)
          .build();

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetMongoUri() throws Exception {
    when(configuration.getMongoConnectionFactory())
        .thenReturn(MongoConfig.builder()
                        .uri("mongodb://localhost:27017/wings")
                        .locksUri("mongodb://localhost:27017/wings")
                        .build());

    when(asymmetricEncryptor.encryptText("mongodb://localhost:27017/wings"))
        .thenReturn("mongodb://localhost:27017/wings".getBytes());
    RestResponse<MongoConfig> restResponse = RESOURCES.client()
                                                 .target(HEALTH_RESOURCE_URL + ConfigurationType.MONGO)
                                                 .request()
                                                 .get(new GenericType<RestResponse<MongoConfig>>() {});

    assertThat(restResponse.getResource()).isNotNull();
    assertThat(restResponse.getResource().getEncryptedUri()).isNotEmpty();
    assertThat(restResponse.getResource().getEncryptedLocksUri()).isNotEmpty();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldGetElasticsearchUri() throws Exception {
    String elasticsearchUri = "http://localhost:9200";

    when(configuration.getElasticsearchConfig())
        .thenReturn(ElasticsearchConfig.builder().uri(elasticsearchUri).indexSuffix("_default").build());
    when(asymmetricEncryptor.encryptText(elasticsearchUri)).thenReturn(elasticsearchUri.getBytes());
    RestResponse<ElasticsearchConfig> restResponse = RESOURCES.client()
                                                         .target(HEALTH_RESOURCE_URL + ConfigurationType.ELASTICSEARCH)
                                                         .request()
                                                         .get(new GenericType<RestResponse<ElasticsearchConfig>>() {});
    assertThat(restResponse.getResource()).isNotNull();
    assertThat(restResponse.getResource().getEncryptedUri()).isNotNull();
    assertThat(restResponse.getResource().getIndexSuffix()).isNotNull();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void shouldGetIsSearchEnabled() {
    when(configuration.isSearchEnabled()).thenReturn(true);
    RestResponse<Boolean> restResponse = RESOURCES.client()
                                             .target(HEALTH_RESOURCE_URL + ConfigurationType.SEARCH_ENABLED)
                                             .request()
                                             .get(new GenericType<RestResponse<Boolean>>() {});
    assertThat(restResponse.getResource()).isEqualTo(true);
  }
}
