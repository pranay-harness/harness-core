/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NEMANJA;
import static io.harness.rule.OwnerRule.RAGHU;

import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.models.VerificationType;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.rule.ResourceTestRule;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVConfigResourceTest extends CvNextGenTestBase {
  @Inject private Injector injector;
  @Inject private static CVConfigResource cvConfigResource = new CVConfigResource();

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder().addResource(cvConfigResource).build();

  private String accountId;
  private String connectorIdentifier;
  private String productName;
  private String monitoringSourceIdentifier;
  private String monitoringSourceName;
  private String serviceInstanceIdentifier;

  @Before
  public void setup() {
    injector.injectMembers(cvConfigResource);
    this.accountId = generateUuid();
    this.connectorIdentifier = generateUuid();
    this.productName = generateUuid();
    this.monitoringSourceIdentifier = generateUuid();
    this.monitoringSourceName = generateUuid();
    this.serviceInstanceIdentifier = generateUuid();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfig() {
    SplunkCVConfig cvConfig = createCVConfig();
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    CVConfig savedCVConfig = saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResource();
    assertThat(savedCVConfig.getUuid()).isNotNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfig_whenConnectorIdentifierIsMissing() {
    SplunkCVConfig cvConfig = createCVConfig();
    cvConfig.setConnectorIdentifier(null);
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));
    assertThat(saveResponse.getStatus()).isEqualTo(500);
    assertThat(
        saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResponseMessages().get(0).getMessage())
        .contains("connectorIdentifier should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfig_whenAccountIdIsMissing() {
    SplunkCVConfig cvConfig = createCVConfig();
    cvConfig.setAccountId(null);
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));
    assertThat(saveResponse.getStatus()).isEqualTo(500);
    assertThat(
        saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResponseMessages().get(0).getMessage())
        .contains("accountId should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfig_whenEnvIdentifierIsMissing() {
    SplunkCVConfig cvConfig = createCVConfig();
    cvConfig.setEnvIdentifier(null);
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));
    assertThat(saveResponse.getStatus()).isEqualTo(500);
    assertThat(
        saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResponseMessages().get(0).getMessage())
        .contains("envIdentifier should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfig_whenServiceIdentifierIsMissing() {
    SplunkCVConfig cvConfig = createCVConfig();
    cvConfig.setServiceIdentifier(null);
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));
    assertThat(saveResponse.getStatus()).isEqualTo(500);
    assertThat(
        saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResponseMessages().get(0).getMessage())
        .contains("serviceIdentifier should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfig_whenProjectIdentifierIsMissing() {
    SplunkCVConfig cvConfig = createCVConfig();
    cvConfig.setProjectIdentifier(null);
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));
    assertThat(saveResponse.getStatus()).isEqualTo(500);
    assertThat(
        saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResponseMessages().get(0).getMessage())
        .contains("projectIdentifier should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfig_whenGroupIdIsMissing() {
    SplunkCVConfig cvConfig = createCVConfig();
    cvConfig.setIdentifier(null);
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));
    assertThat(saveResponse.getStatus()).isEqualTo(500);
    assertThat(
        saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResponseMessages().get(0).getMessage())
        .contains("identifier should not be null");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSaveCVConfig_whenNameIsMissing() {
    SplunkCVConfig cvConfig = createCVConfig();
    cvConfig.setMonitoringSourceName(null);
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));
    assertThat(saveResponse.getStatus()).isEqualTo(500);
    assertThat(
        saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResponseMessages().get(0).getMessage())

        .contains("monitoringSourceName should not be null");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testSaveCVConfigBatch() {
    List<CVConfig> cvConfigs = Arrays.asList(createCVConfig(), createCVConfig());
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config/batch")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfigs, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    List<CVConfig> savedCVConfigs =
        saveResponse.readEntity(new GenericType<RestResponse<List<CVConfig>>>() {}).getResource();
    assertThat(savedCVConfigs.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testUpdateCVConfig() {
    SplunkCVConfig cvConfig = createCVConfig();

    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    CVConfig savedCVConfig = saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResource();
    savedCVConfig.setProductName("UpdatedProductName");

    Response updateResponse = RESOURCES.client()
                                  .target("http://localhost:9998/cv-config")
                                  .path(savedCVConfig.getUuid())
                                  .queryParam("accountId", accountId)
                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                  .put(entity(savedCVConfig, MediaType.APPLICATION_JSON_TYPE));

    assertThat(updateResponse.getStatus()).isEqualTo(200);
    CVConfig updatedCVConfig = updateResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResource();
    assertThat(updatedCVConfig.getProductName()).isEqualTo("UpdatedProductName");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testUpdateCVConfigBatch() {
    List<CVConfig> cvConfigs = Arrays.asList(createCVConfig(), createCVConfig());
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config/batch")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfigs, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    List<CVConfig> savedCVConfigs =
        saveResponse.readEntity(new GenericType<RestResponse<List<CVConfig>>>() {}).getResource();
    assertThat(savedCVConfigs.size()).isEqualTo(2);

    savedCVConfigs.get(0).setProductName("ProductName1");
    savedCVConfigs.get(1).setProductName("ProductName2");

    Response updateResponse = RESOURCES.client()
                                  .target("http://localhost:9998/cv-config/batch")
                                  .queryParam("accountId", accountId)
                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                  .put(entity(savedCVConfigs, MediaType.APPLICATION_JSON_TYPE));

    assertThat(updateResponse.getStatus()).isEqualTo(200);
    List<CVConfig> updatedCVConfigs =
        updateResponse.readEntity(new GenericType<RestResponse<List<CVConfig>>>() {}).getResource();
    assertThat(updatedCVConfigs.size()).isEqualTo(2);
    assertThat(updatedCVConfigs.get(0).getProductName()).isEqualTo("ProductName1");
    assertThat(updatedCVConfigs.get(1).getProductName()).isEqualTo("ProductName2");
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetCVConfig() {
    SplunkCVConfig cvConfig = createCVConfig();
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    CVConfig savedCVConfig = saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResource();
    assertThat(savedCVConfig.getUuid()).isNotNull();

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/cv-config")
                            .path(savedCVConfig.getUuid())
                            .queryParam("accountId", accountId)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    CVConfig retrievedCVConfig = response.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResource();
    assertThat(retrievedCVConfig.getUuid()).isEqualTo(savedCVConfig.getUuid());
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testDeleteCVConfig() {
    SplunkCVConfig cvConfig = createCVConfig();
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config")
                                .queryParam("accountId", accountId)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfig, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    CVConfig savedCVConfig = saveResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {}).getResource();
    assertThat(savedCVConfig.getUuid()).isNotNull();

    Response deleteResponse = RESOURCES.client()
                                  .target("http://localhost:9998/cv-config")
                                  .path(savedCVConfig.getUuid())
                                  .queryParam("accountId", accountId)
                                  .request(MediaType.APPLICATION_JSON_TYPE)
                                  .delete();

    assertThat(deleteResponse.getStatus()).isEqualTo(204);
    assertThat(deleteResponse.readEntity(new GenericType<RestResponse<CVConfig>>() {})).isNull();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testListCVConfigs() {
    List<CVConfig> cvConfigs = Arrays.asList(createCVConfig(), createCVConfig());
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config/batch")
                                .queryParam("accountId", accountId)
                                .queryParam("connectorIdentifier", connectorIdentifier)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfigs, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    List<CVConfig> savedCVConfigs =
        saveResponse.readEntity(new GenericType<RestResponse<List<CVConfig>>>() {}).getResource();
    assertThat(savedCVConfigs.size()).isEqualTo(2);

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/cv-config/list")
                            .queryParam("accountId", accountId)
                            .queryParam("connectorIdentifier", connectorIdentifier)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    List<CVConfig> retrievedCVConfigs =
        response.readEntity(new GenericType<RestResponse<List<CVConfig>>>() {}).getResource();
    assertThat(retrievedCVConfigs.get(0).getUuid()).isEqualTo(savedCVConfigs.get(0).getUuid());
    assertThat(retrievedCVConfigs.get(1).getUuid()).isEqualTo(savedCVConfigs.get(1).getUuid());
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testGetProductNames() {
    List<CVConfig> cvConfigs = Arrays.asList(createCVConfig(), createCVConfig());
    cvConfigs.get(1).setProductName(generateUuid());
    Response saveResponse = RESOURCES.client()
                                .target("http://localhost:9998/cv-config/batch")
                                .queryParam("accountId", accountId)
                                .queryParam("connectorIdentifier", connectorIdentifier)
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .post(entity(cvConfigs, MediaType.APPLICATION_JSON_TYPE));

    assertThat(saveResponse.getStatus()).isEqualTo(200);
    List<CVConfig> savedCVConfigs =
        saveResponse.readEntity(new GenericType<RestResponse<List<CVConfig>>>() {}).getResource();
    assertThat(savedCVConfigs.size()).isEqualTo(2);

    Response response = RESOURCES.client()
                            .target("http://localhost:9998/cv-config/product-names")
                            .queryParam("accountId", accountId)
                            .queryParam("connectorIdentifier", connectorIdentifier)
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();

    assertThat(response.getStatus()).isEqualTo(200);
    List<String> retrievedProductNames =
        response.readEntity(new GenericType<RestResponse<List<String>>>() {}).getResource();
    assertThat(retrievedProductNames).contains(savedCVConfigs.get(0).getProductName());
    assertThat(retrievedProductNames).contains(savedCVConfigs.get(1).getProductName());
  }

  private SplunkCVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(serviceInstanceIdentifier);
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setServiceIdentifier(generateUuid());
    cvConfig.setEnvIdentifier(generateUuid());
    cvConfig.setProjectIdentifier(generateUuid());
    cvConfig.setOrgIdentifier(generateUuid());
    cvConfig.setIdentifier(monitoringSourceIdentifier);
    cvConfig.setMonitoringSourceName(monitoringSourceName);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(productName);
    cvConfig.setCreatedAt(1);
  }
}
