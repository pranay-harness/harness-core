package software.wings.integration;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.service.impl.security.SecretText;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;

import java.io.IOException;
import java.util.Collections;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * An integration test to exercise encrypted text/file create/update/delete code path.
 *
 * @author marklu on 10/16/18
 */
public class SecretManagerIntegrationTest extends BaseIntegrationTest {
  @Before
  public void setUp() {
    super.loginAdminUser();
  }

  @Test
  public void test_crudSecret_shouldSucceed() {
    // 1. Create a new secret text
    WebTarget target = client.target(API_BASE + "/secrets/add-secret?accountId=" + accountId);
    RestResponse<String> createResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(generateSecretText("MySecret"), APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, createResponse.getResponseMessages().size());
    String secretUuid = createResponse.getResource();
    assertNotNull(secretUuid);

    // 2. Update the secret text.
    target = client.target(API_BASE + "/secrets/update-secret?accountId=" + accountId + "&uuid=" + secretUuid);
    RestResponse<Boolean> updateResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(generateSecretText("MySecret2"), APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, updateResponse.getResponseMessages().size());
    assertTrue(updateResponse.getResource());

    // 2. Delete the secret text.
    target = client.target(API_BASE + "/secrets/delete-secret?accountId=" + accountId + "&uuid=" + secretUuid);
    RestResponse<Boolean> deleteResponse =
        getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Boolean>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, deleteResponse.getResponseMessages().size());
    assertTrue(deleteResponse.getResource());
  }

  @Test
  public void test_crudEncryptedFile_shouldSucceed() throws Exception {
    // 1. Create a new encrypted file.
    WebTarget target = client.target(API_BASE + "/secrets/add-file?accountId=" + accountId);
    RestResponse<String> createResponse = getRequestBuilderWithAuthHeader(target).post(
        generateSecretFileForm("TestSecretFile", null), new GenericType<RestResponse<String>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, createResponse.getResponseMessages().size());
    String secretUuid = createResponse.getResource();
    assertNotNull(secretUuid);

    // 2. Update the encrypted file.
    target = client.target(API_BASE + "/secrets/update-file?accountId=" + accountId);
    RestResponse<Boolean> updateResponse = getRequestBuilderWithAuthHeader(target).post(
        generateSecretFileForm("TestSecretFile", secretUuid), new GenericType<RestResponse<Boolean>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, updateResponse.getResponseMessages().size());
    assertTrue(updateResponse.getResource());

    // 2. Delete the encrypted file.
    target = client.target(API_BASE + "/secrets/delete-file?accountId=" + accountId + "&uuid=" + secretUuid);
    RestResponse<Boolean> deleteResponse =
        getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Boolean>>() {});
    // Verify vault config was successfully created.
    assertEquals(0, deleteResponse.getResponseMessages().size());
    assertTrue(deleteResponse.getResource());
  }

  private Entity<MultiPart> generateSecretFileForm(String fileName, String uuid) throws Exception {
    FormDataBodyPart bodyPart = new FormDataBodyPart("file", "Test File Content!");
    FormDataMultiPart multiPart = new FormDataMultiPart();
    multiPart.field("name", fileName).field("usageRestrictions", generateUsageRestrictionsJson()).bodyPart(bodyPart);
    if (uuid != null) {
      multiPart.field("uuid", uuid);
    }
    return entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE);
  }

  private String generateUsageRestrictionsJson() throws IOException {
    UsageRestrictions usageRestrictions = generateUsageRestrictions();
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(usageRestrictions);
  }

  private SecretText generateSecretText(String secret) {
    return SecretText.builder()
        .name("TestSecret1")
        .value(secret)
        .usageRestrictions(generateUsageRestrictions())
        .build();
  }

  private UsageRestrictions generateUsageRestrictions() {
    return UsageRestrictions.builder().appEnvRestrictions(Collections.singleton(generateAppEnvRestriction())).build();
  }

  private AppEnvRestriction generateAppEnvRestriction() {
    return AppEnvRestriction.builder()
        .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
        .envFilter(EnvFilter.builder().filterTypes(Collections.singleton(EnvFilter.FilterType.NON_PROD)).build())
        .build();
  }
}
