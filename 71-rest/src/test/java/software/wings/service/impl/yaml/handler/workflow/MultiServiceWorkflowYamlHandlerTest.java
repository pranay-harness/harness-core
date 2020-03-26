package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.RAMA;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.MULTI_SERVICE_INVALID_YAML_CONTENT;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.MULTI_SERVICE_INVALID_YAML_FILE_PATH;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.MULTI_SERVICE_VALID_YAML_CONTENT;
import static software.wings.service.impl.yaml.handler.workflow.WorkflowYamlConstant.MULTI_SERVICE_VALID_YAML_FILE_PATH;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.beans.Workflow;
import software.wings.beans.concurrency.ConcurrencyStrategy.UnitType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.utils.WingsTestConstants.MockChecker;
import software.wings.yaml.workflow.MultiServiceWorkflowYaml;

/**
 * @author rktummala on 1/10/18
 */
public class MultiServiceWorkflowYamlHandlerTest extends BaseWorkflowYamlHandlerTest {
  private String workflowName = "multiService";

  @InjectMocks @Inject private MultiServiceWorkflowYamlHandler yamlHandler;
  @Mock private LimitCheckerFactory limitCheckerFactory;

  @Before
  public void runBeforeTest() {
    setup(MULTI_SERVICE_VALID_YAML_FILE_PATH, workflowName);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testCRUDAndGet() throws Exception {
    when(limitCheckerFactory.getInstance(new Action(Mockito.anyString(), ActionType.CREATE_WORKFLOW)))
        .thenReturn(new MockChecker(true, ActionType.CREATE_WORKFLOW));

    ChangeContext<MultiServiceWorkflowYaml> changeContext =
        getChangeContext(MULTI_SERVICE_VALID_YAML_CONTENT, MULTI_SERVICE_VALID_YAML_FILE_PATH, yamlHandler);

    MultiServiceWorkflowYaml yamlObject =
        (MultiServiceWorkflowYaml) getYaml(MULTI_SERVICE_VALID_YAML_CONTENT, MultiServiceWorkflowYaml.class);
    changeContext.setYaml(yamlObject);

    Workflow workflow = yamlHandler.upsertFromYaml(changeContext, asList(changeContext));
    assertThat(workflow).isNotNull();
    assertThat(workflowName).isEqualTo(workflow.getName());
    assertThat(workflow.getOrchestrationWorkflow()).isNotNull();
    assertThat(workflow.getOrchestrationWorkflow().getConcurrencyStrategy()).isNotNull();
    assertThat(workflow.getOrchestrationWorkflow().getConcurrencyStrategy().getUnitType()).isEqualTo(UnitType.NONE);

    MultiServiceWorkflowYaml yaml = yamlHandler.toYaml(workflow, APP_ID);
    assertThat(yaml).isNotNull();
    assertThat(yaml.getType()).isEqualTo("MULTI_SERVICE");

    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(MULTI_SERVICE_VALID_YAML_CONTENT);

    Workflow savedWorkflow = workflowService.readWorkflowByName(APP_ID, workflowName);
    // TODO find out why this couldn't be called
    //    Workflow savedWorkflow = yamlHandler.get(ACCOUNT_ID, validYamlFilePath);
    assertThat(savedWorkflow).isNotNull();
    assertThat(workflowName).isEqualTo(savedWorkflow.getName());

    yamlHandler.delete(changeContext);

    Workflow deletedWorkflow = yamlHandler.get(ACCOUNT_ID, MULTI_SERVICE_VALID_YAML_FILE_PATH);
    assertThat(deletedWorkflow).isNull();
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testFailures() throws Exception {
    testFailures(MULTI_SERVICE_VALID_YAML_CONTENT, MULTI_SERVICE_VALID_YAML_FILE_PATH,
        MULTI_SERVICE_INVALID_YAML_CONTENT, MULTI_SERVICE_INVALID_YAML_FILE_PATH, yamlHandler,
        MultiServiceWorkflowYaml.class);
  }
}
