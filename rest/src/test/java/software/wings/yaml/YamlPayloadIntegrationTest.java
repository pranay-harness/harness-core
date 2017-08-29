package software.wings.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.integration.BaseIntegrationTest;

import java.io.IOException;
import java.util.Arrays;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * Created by bsollish on 8/10/17.
 */
public class YamlPayloadIntegrationTest extends BaseIntegrationTest {
  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_NAME_POST = "TestAppPOST_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION_POST = "stuffPOST";
  private final String TEST_YAML_POST =
      "--- # app.yaml for new Application\nname: " + TEST_NAME_POST + "\ndescription: " + TEST_DESCRIPTION_POST;
  private final String TEST_NAME_PUT = "TestAppPUT_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION_PUT = "stuffPUT";
  private final String TEST_YAML_PUT =
      "--- # app.yaml for new Application\nname: " + TEST_NAME_PUT + "\ndescription: " + TEST_DESCRIPTION_PUT;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(SettingAttribute.class));
  }

  @Test
  public void testSaveFromYamlAndUpdateFromYaml() throws IOException {
    //-------------- POST (Save) ------------------

    YamlPayload ypPost = new YamlPayload(TEST_YAML_POST);
    WebTarget targetPost = client.target(API_BASE + "/apps/yaml?accountId=" + accountId);
    RestResponse<Application> restResponsePost =
        getRequestBuilderWithAuthHeader(targetPost)
            .post(Entity.entity(ypPost, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Application>>() {});

    Assert.assertEquals(0, restResponsePost.getResponseMessages().size());

    Application appPost = restResponsePost.getResource();

    assertThat(appPost).isNotEqualTo(null);
    assertThat(appPost.getName()).isEqualTo(TEST_NAME_POST);
    assertThat(appPost.getDescription()).isEqualTo(TEST_DESCRIPTION_POST);
    assertThat(appPost.getAccountId()).isEqualTo(accountId);

    String appId = appPost.getAppId();

    //-------------- PUT (Update) ------------------

    YamlPayload ypPut = new YamlPayload(TEST_YAML_PUT);
    WebTarget targetPut = client.target(API_BASE + "/apps/yaml/" + appId);
    RestResponse<Application> restResponsePut = getRequestBuilderWithAuthHeader(targetPut).put(
        Entity.entity(ypPut, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Application>>() {});

    Assert.assertEquals(0, restResponsePut.getResponseMessages().size());

    Application appPut = restResponsePut.getResource();

    assertThat(appPut).isNotEqualTo(null);
    assertThat(appPut.getName()).isEqualTo(TEST_NAME_PUT);
    assertThat(appPut.getDescription()).isEqualTo(TEST_DESCRIPTION_PUT);
    assertThat(appPut.getAccountId()).isEqualTo(accountId);

    //-------------- Cleanup: DELETE ------------------

    WebTarget targetDelete = client.target(API_BASE + "/apps/" + appId);
    RestResponse restResponseDelete =
        getRequestBuilderWithAuthHeader(targetDelete).delete(new GenericType<RestResponse>() {});
  }
}
