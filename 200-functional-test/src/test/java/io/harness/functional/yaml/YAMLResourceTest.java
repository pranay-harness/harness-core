/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.yaml;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import software.wings.yaml.FileOperationStatus;
import software.wings.yaml.YamlOperationResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(DX)
@Slf4j
public class YAMLResourceTest extends AbstractFunctionalTest {
  private YamlOperationResponse performYamlOperation(final Response response) {
    assertThat(response.getStatusCode() == HttpStatus.SC_OK).isTrue();
    assertThat(response.body()).isNotNull();
    YamlOperationResponse yamlOperationResponse = null;
    try {
      JSONObject responseArray = new JSONObject(response.body().print());
      assertThat(responseArray).isNotNull();
      final JSONObject resource = responseArray.getJSONObject("resource");
      assertThat(resource).isNotNull();
      ObjectMapper mapper = new ObjectMapper();
      yamlOperationResponse = mapper.readValue(resource.toString(), YamlOperationResponse.class);
    } catch (JSONException | IOException err) {
      log.error(err.toString());
    }
    return yamlOperationResponse;
  }

  private Response setupTestData(final String appName) {
    File file = null;

    file = new File("200-functional-test/src/test/resources/io/harness/yaml/" + appName + ".zip");

    return Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", getAccount().getUuid())
        .contentType(MULTIPART_FORM_DATA)
        .multiPart(file)
        .post("/setup-as-code/yaml/upsert-entities");
  }

  @Test
  @Owner(developers = VARDAN_BANSAL, intermittent = true)
  @Category(FunctionalTests.class)
  public void test_upsertEntities() {
    final Response response = setupTestData("SampleApp");
    final YamlOperationResponse yamlOperationResponse = performYamlOperation(response);
    assertThat(yamlOperationResponse).isNotNull();
    assertThat(yamlOperationResponse.getResponseStatus()).isEqualByComparingTo(YamlOperationResponse.Status.SUCCESS);
    assertThat(yamlOperationResponse.getFilesStatus()).isNotEmpty();
  }

  @Test
  @Owner(developers = VARDAN_BANSAL, intermittent = true)
  @Category(FunctionalTests.class)
  public void test_deleteEntities_allFilesDeleted() {
    setupTestData("SampleApp");

    final Response response =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam("accountId", getAccount().getUuid())
            .queryParam("filePaths", "Setup/Applications/test app/Index.yaml")
            .queryParam("filePaths", "Setup/Applications/test app/Services/test2/Manifests/Files/values.yaml")
            .delete("/setup-as-code/yaml/delete-entities");

    final YamlOperationResponse yamlOperationResponse = performYamlOperation(response);
    assertThat(yamlOperationResponse).isNotNull();
    assertThat(yamlOperationResponse.getResponseStatus()).isEqualByComparingTo(YamlOperationResponse.Status.SUCCESS);
    final List<FileOperationStatus> filesStatusList = yamlOperationResponse.getFilesStatus();
    assertThat(filesStatusList).isNotEmpty();
    assertThat(filesStatusList.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = VARDAN_BANSAL, intermittent = true)
  @Category(FunctionalTests.class)
  public void test_deleteEntities_fewFilesDeleted() {
    setupTestData("SampleApp");

    final String FILES_DELETED_SUCCESSFULLY = "Setup/Applications/test app/Index.yaml";
    final String FILES_SKIPPED = "Setup/Applications/Sample App/Index.yaml";
    final String FILES_FAILING_DELETION = "Setup/Applications/test app/apple.yaml";

    final Response response = Setup.portal()
                                  .auth()
                                  .oauth2(bearerToken)
                                  .queryParam("accountId", getAccount().getUuid())
                                  .queryParam("filePaths", FILES_DELETED_SUCCESSFULLY)
                                  .queryParam("filePaths", FILES_SKIPPED)
                                  .queryParam("filePaths", FILES_FAILING_DELETION)
                                  .delete("/setup-as-code/yaml/delete-entities");

    final YamlOperationResponse yamlOperationResponse = performYamlOperation(response);
    assertThat(yamlOperationResponse).isNotNull();
    assertThat(yamlOperationResponse.getResponseStatus()).isEqualByComparingTo(YamlOperationResponse.Status.FAILED);
    final List<FileOperationStatus> filesStatusList = yamlOperationResponse.getFilesStatus();
    assertThat(filesStatusList).isNotEmpty();
    final List<FileOperationStatus> failedFiles =
        filesStatusList.stream()
            .filter(fileOperationStatus -> fileOperationStatus.getStatus().equals(FileOperationStatus.Status.FAILED))
            .collect(Collectors.toList());
    final List<FileOperationStatus> successfulFiles =
        filesStatusList.stream()
            .filter(fileOperationStatus -> fileOperationStatus.getStatus().equals(FileOperationStatus.Status.SUCCESS))
            .collect(Collectors.toList());
    assertThat(failedFiles.size()).isEqualTo(1);
    assertThat(failedFiles.get(0).getYamlFilePath().equals(FILES_FAILING_DELETION));
    assertThat(successfulFiles.size()).isEqualTo(2);
    assertThat(successfulFiles.get(0).getYamlFilePath().equals(FILES_DELETED_SUCCESSFULLY));
  }

  @Test
  @Owner(developers = VARDAN_BANSAL, intermittent = true)
  @Category(FunctionalTests.class)
  public void test_upsertDeleteEntities_emptyApp() {
    setupTestData("EmptyApp");

    final Response response = Setup.portal()
                                  .auth()
                                  .oauth2(bearerToken)
                                  .queryParam("accountId", getAccount().getUuid())
                                  .queryParam("filePaths", "Setup/someFile.yaml")
                                  .queryParam("filePaths", "Setup/anotherFile.yaml")
                                  .delete("/setup-as-code/yaml/delete-entities");

    final YamlOperationResponse yamlOperationResponse = performYamlOperation(response);
    assertThat(yamlOperationResponse).isNotNull();
    assertThat(yamlOperationResponse.getResponseStatus()).isEqualByComparingTo(YamlOperationResponse.Status.FAILED);
    assertThat(yamlOperationResponse.getFilesStatus()).isNull();
  }

  @Test
  @Owner(developers = VARDAN_BANSAL, intermittent = true)
  @Category(FunctionalTests.class)
  public void test_upsertEntity() {
    final String yamlFilePath = "Setup/Applications/test app/Index.yaml";
    final Response response = Setup.portal()
                                  .auth()
                                  .oauth2(bearerToken)
                                  .queryParam("accountId", getAccount().getUuid())
                                  .queryParam("yamlFilePath", yamlFilePath)
                                  .contentType("multipart/form-data")
                                  .multiPart("yamlContent",
                                      "harnessApiVersion: '1.0'\n"
                                          + "type: APPLICATION")
                                  .post("/setup-as-code/yaml/upsert-entity");
    assertThat(response.getStatusCode() == HttpStatus.SC_OK).isTrue();
    assertThat(response.body()).isNotNull();
    FileOperationStatus fileOperationStatus;
    try {
      JSONObject responseObj = new JSONObject(response.body().print());
      assertThat(responseObj).isNotNull();
      final JSONObject resource = responseObj.getJSONObject("resource");
      assertThat(resource).isNotNull();
      ObjectMapper mapper = new ObjectMapper();
      fileOperationStatus = mapper.readValue(resource.toString(), FileOperationStatus.class);
      assertThat(fileOperationStatus.getYamlFilePath().equals(yamlFilePath));
      assertThat(fileOperationStatus.getStatus().equals(FileOperationStatus.Status.SUCCESS));
    } catch (JSONException | IOException err) {
      log.error(err.toString());
    }
  }
}
