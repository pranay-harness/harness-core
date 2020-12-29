package io.harness.functional.ci;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.CIManagerExecutor;
import io.harness.testframework.restutils.NGPipelineRestUtils;

import io.restassured.RestAssured;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIPipelineCRUDFunctionalTest extends CategoryTest {
  private static final String ALPN_JAR =
      "org/mortbay/jetty/alpn/alpn-boot/8.1.13.v20181017/alpn-boot-8.1.13.v20181017.jar";
  private static final String ALPN = "/home/jenkins/maven-repositories/0/";

  private static final String accountIdentifier = "accountIdentifier";
  private static final String orgIdentifier = "orgIdentifier";
  private static final String projectIdentifier = "projectIdentifier";
  private static final String pipelineNamePlaceholder = "NAME_PLACEHOLDER";
  private static final String pipelineDescriptionPlaceholder = "DESCRIPTION_PLACEHOLDER";

  @BeforeClass
  public static void setup() throws IOException {
    RestAssured.useRelaxedHTTPSValidation();
    CIManagerExecutor.ensureCIManager(CIPipelineCRUDFunctionalTest.class, ALPN, ALPN_JAR);
  }

  @Test
  @Owner(developers = ALEKSANDAR, intermittent = true)
  @Category({FunctionalTests.class})
  public void shouldTestCRUDPipelineFlow() throws IOException {
    String pipelineTemplate = IOUtils.toString(
        CIPipelineCRUDFunctionalTest.class.getResourceAsStream("pipeline.yml"), StandardCharsets.UTF_8);
    String pipelineName = UUID.randomUUID().toString();

    String pipeline = pipelineTemplate.replace(pipelineNamePlaceholder, pipelineName)
                          .replace(pipelineDescriptionPlaceholder, UUID.randomUUID().toString());

    // Create
    String id = NGPipelineRestUtils.createPipeline(accountIdentifier, orgIdentifier, projectIdentifier, pipeline);
    assertThat(id).isNotNull().isEqualTo(pipelineName);

    // Read
    Map<String, Object> ngPipelineResponseDTO =
        NGPipelineRestUtils.readPipeline(accountIdentifier, orgIdentifier, projectIdentifier, id);
    String yamlPipeline = (String) ngPipelineResponseDTO.get("yamlPipeline");
    assertThat(yamlPipeline).isEqualTo(pipeline);

    String updatedPipeline = pipelineTemplate.replace(pipelineNamePlaceholder, pipelineName)
                                 .replace(pipelineDescriptionPlaceholder, UUID.randomUUID().toString());

    // Update
    String updatedPipelineId =
        NGPipelineRestUtils.updatePipeline(accountIdentifier, orgIdentifier, projectIdentifier, id, updatedPipeline);
    assertThat(updatedPipelineId).isEqualTo(id);

    // Delete
    Boolean deletePipeline =
        NGPipelineRestUtils.deletePipeline(accountIdentifier, orgIdentifier, projectIdentifier, id);
    assertThat(deletePipeline).isTrue();
  }
}
