package software.wings.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BUILD_INVALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BUILD_INVALID_YAML_FILE_PATH;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BUILD_VALID_YAML_CONTENT;
import static software.wings.yaml.handler.workflow.WorkflowYamlConstant.BUILD_VALID_YAML_FILE_PATH;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.HarnessException;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.workflow.BuildWorkflowYamlHandler;
import software.wings.utils.WingsTestConstants.MockChecker;
import software.wings.yaml.workflow.BuildWorkflowYaml;

import java.io.IOException;

/**
 * @author rktummala on 1/10/18
 */
public class BuildWorkflowYamlHandlerTest extends BaseWorkflowYamlHandlerTest {
  private String workflowName = "build";

  @InjectMocks @Inject private BuildWorkflowYamlHandler yamlHandler;
  @Mock private LimitCheckerFactory limitCheckerFactory;

  @Before
  public void runBeforeTest() {
    setup(BUILD_VALID_YAML_FILE_PATH, workflowName);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws HarnessException, IOException {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    ChangeContext<BuildWorkflowYaml> changeContext =
        getChangeContext(BUILD_VALID_YAML_CONTENT, BUILD_VALID_YAML_FILE_PATH, yamlHandler);

    BuildWorkflowYaml yamlObject = (BuildWorkflowYaml) getYaml(BUILD_VALID_YAML_CONTENT, BuildWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(workflow).isNotNull();
    assertThat(workflowName).isEqualTo(workflow.getName());

    BuildWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo("BUILD");

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(BUILD_VALID_YAML_CONTENT);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(savedWorkflow).isNotNull();
    assertThat(workflowName).isEqualTo(savedWorkflow.getName());

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, BUILD_VALID_YAML_FILE_PATH);
    assertThat(deletedWorkflow).isNull();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testFailures() throws HarnessException, IOException {
    testFailures(BUILD_VALID_YAML_CONTENT, BUILD_VALID_YAML_FILE_PATH, BUILD_INVALID_YAML_CONTENT,
        BUILD_INVALID_YAML_FILE_PATH, yamlHandler, BuildWorkflowYaml.class);
  }
}
