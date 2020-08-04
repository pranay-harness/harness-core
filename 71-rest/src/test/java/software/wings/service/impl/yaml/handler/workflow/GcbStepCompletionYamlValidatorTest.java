package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.AGORODETKI;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.singletonMap;
import static org.junit.runners.Parameterized.Parameters;
import static software.wings.service.impl.yaml.handler.workflow.StepCompletionYamlValidatorFactory.getValidatorForStepType;

import com.google.common.collect.ImmutableMap;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.wings.exception.IncompleteStateException;
import software.wings.sm.StepType;
import software.wings.yaml.workflow.StepYaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
public class GcbStepCompletionYamlValidatorTest {
  private final StepYaml stepYaml;
  private final StepCompletionYamlValidator validator;
  @Rule public final ExpectedException exception = ExpectedException.none();

  public GcbStepCompletionYamlValidatorTest(
      StepYaml stepYaml, String message, Class<? extends Exception> expectedException) {
    this.stepYaml = stepYaml;
    this.validator = getValidatorForStepType(StepType.valueOf("GCB"));
    if (expectedException != null) {
      exception.expect(expectedException);
      exception.expectMessage(message);
    }
  }

  @Parameters
  public static Collection<Object[]> data() {
    // parameter
    Map<String, Object> propertiesWithoutGcbOptions = Collections.emptyMap();
    // parameter
    Map<String, Object> propertiesWithNullNonTemplatizedGcpConfig =
        ImmutableMap.of("templateExpressions", Collections.EMPTY_LIST, "gcbOptions", singletonMap("gcpConfigId", null));
    // parameter
    Map<String, Object> propertiesWithoutSpecSource = new HashMap<>();
    Map<String, Object> gcbOptions = new HashMap<>();
    gcbOptions.put("gcpConfigId", "id");
    gcbOptions.put("specSource", null);
    propertiesWithoutSpecSource.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithoutSpecSource.put("gcbOptions", gcbOptions);
    // parameter
    Map<String, Object> propertiesWithNullInlineSpec = new HashMap<>();
    Map<String, Object> inlineOptions = new HashMap<>();
    inlineOptions.put("gcpConfigId", "id");
    inlineOptions.put("specSource", "INLINE");
    inlineOptions.put("inlineSpec", null);
    propertiesWithNullInlineSpec.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithNullInlineSpec.put("gcbOptions", inlineOptions);

    // parameter
    Map<String, Object> propertiesWithNullTriggerSpec = new HashMap<>();
    Map<String, Object> triggerOptions = new HashMap<>();
    triggerOptions.put("gcpConfigId", "id");
    triggerOptions.put("specSource", "TRIGGER");
    triggerOptions.put("triggerSpec", null);
    propertiesWithNullTriggerSpec.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithNullTriggerSpec.put("gcbOptions", triggerOptions);

    // parameter
    Map<String, Object> propertiesTriggerSpecWithoutSource = new HashMap<>();
    Map<String, Object> triggerOptionsWithoutSource = new HashMap<>();
    triggerOptionsWithoutSource.put("gcpConfigId", "id");
    triggerOptionsWithoutSource.put("specSource", "TRIGGER");
    triggerOptionsWithoutSource.put("triggerSpec", singletonMap("source", null));
    propertiesTriggerSpecWithoutSource.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesTriggerSpecWithoutSource.put("gcbOptions", triggerOptionsWithoutSource);

    // parameter
    Map<String, Object> propertiesTriggerSpecWithoutName = new HashMap<>();
    Map<String, Object> triggerOptionsWithoutName = new HashMap<>();
    Map<String, Object> triggerSpec = new HashMap<>();
    triggerSpec.put("source", "TAG");
    triggerSpec.put("name", null);
    triggerOptionsWithoutName.put("gcpConfigId", "id");
    triggerOptionsWithoutName.put("specSource", "TRIGGER");
    triggerOptionsWithoutName.put("triggerSpec", triggerSpec);
    propertiesTriggerSpecWithoutName.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesTriggerSpecWithoutName.put("gcbOptions", triggerOptionsWithoutName);

    // parameter
    Map<String, Object> propertiesTriggerSpecWithoutSourceId = new HashMap<>();
    Map<String, Object> triggerOptionsWithoutSourceId = new HashMap<>();
    Map<String, Object> triggerSpecWithoutId = new HashMap<>();
    triggerSpecWithoutId.put("source", "TAG");
    triggerSpecWithoutId.put("sourceId", null);
    triggerSpecWithoutId.put("name", "triggerName");
    triggerOptionsWithoutSourceId.put("gcpConfigId", "id");
    triggerOptionsWithoutSourceId.put("specSource", "TRIGGER");
    triggerOptionsWithoutSourceId.put("triggerSpec", triggerSpecWithoutId);
    propertiesTriggerSpecWithoutSourceId.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesTriggerSpecWithoutSourceId.put("gcbOptions", triggerOptionsWithoutSourceId);

    // parameter
    Map<String, Object> propertiesWithNullRepoSpec = new HashMap<>();
    Map<String, Object> repoOptions = new HashMap<>();
    repoOptions.put("gcpConfigId", "id");
    repoOptions.put("specSource", "REMOTE");
    repoOptions.put("repositorySpec", null);
    propertiesWithNullRepoSpec.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithNullRepoSpec.put("gcbOptions", repoOptions);

    // parameter
    Map<String, Object> propertiesWithNullGitConfig = new HashMap<>();
    Map<String, Object> repoOptionsWithoutGitConfig = new HashMap<>();
    repoOptionsWithoutGitConfig.put("gcpConfigId", "id");
    repoOptionsWithoutGitConfig.put("specSource", "REMOTE");
    repoOptionsWithoutGitConfig.put("repositorySpec", EMPTY_MAP);
    propertiesWithNullGitConfig.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithNullGitConfig.put("gcbOptions", repoOptionsWithoutGitConfig);

    // parameter
    Map<String, Object> propertiesWithoutFileSource = new HashMap<>();
    Map<String, Object> repoOptionsWithoutFileSource = new HashMap<>();
    Map<String, Object> repositorySpecWithoutFileSource = new HashMap<>();
    repositorySpecWithoutFileSource.put("gitConfigName", "config");
    repositorySpecWithoutFileSource.put("fileSource", null);
    repoOptionsWithoutFileSource.put("gcpConfigId", "id");
    repoOptionsWithoutFileSource.put("specSource", "REMOTE");
    repoOptionsWithoutFileSource.put("repositorySpec", repositorySpecWithoutFileSource);
    propertiesWithoutFileSource.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithoutFileSource.put("gcbOptions", repoOptionsWithoutFileSource);

    // parameter
    Map<String, Object> propertiesWithoutFilePath = new HashMap<>();
    Map<String, Object> repoOptionsWithoutFilePath = new HashMap<>();
    Map<String, Object> repositorySpecWithoutFilePath = new HashMap<>();
    repositorySpecWithoutFilePath.put("gitConfigName", "config");
    repositorySpecWithoutFilePath.put("fileSource", "BRANCH");
    repositorySpecWithoutFilePath.put("filePath", null);
    repoOptionsWithoutFilePath.put("gcpConfigId", "id");
    repoOptionsWithoutFilePath.put("specSource", "REMOTE");
    repoOptionsWithoutFilePath.put("repositorySpec", repositorySpecWithoutFilePath);
    propertiesWithoutFilePath.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithoutFilePath.put("gcbOptions", repoOptionsWithoutFilePath);

    // parameter
    Map<String, Object> propertiesWithoutSourceId = new HashMap<>();
    Map<String, Object> repoOptionsWithoutSourceId = new HashMap<>();
    Map<String, Object> repositorySpecWithoutSourceId = new HashMap<>();
    repositorySpecWithoutSourceId.put("gitConfigName", "config");
    repositorySpecWithoutSourceId.put("fileSource", "BRANCH");
    repositorySpecWithoutSourceId.put("filePath", "path");
    repositorySpecWithoutSourceId.put("sourceId", null);
    repoOptionsWithoutSourceId.put("gcpConfigId", "id");
    repoOptionsWithoutSourceId.put("specSource", "REMOTE");
    repoOptionsWithoutSourceId.put("repositorySpec", repositorySpecWithoutSourceId);
    propertiesWithoutSourceId.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithoutSourceId.put("gcbOptions", repoOptionsWithoutSourceId);

    // parameter
    Map<String, Object> propertiesWithValidInlineSpec = new HashMap<>();
    Map<String, Object> inline = new HashMap<>();
    inline.put("gcpConfigId", "id");
    inline.put("specSource", "INLINE");
    inline.put("inlineSpec", "inlineSpec");
    propertiesWithValidInlineSpec.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithValidInlineSpec.put("gcbOptions", inline);

    // parameter
    Map<String, Object> propertiesWithValidRemoteSpec = new HashMap<>();
    Map<String, Object> validRepoOptions = new HashMap<>();
    Map<String, Object> validRepoSpec = new HashMap<>();
    validRepoSpec.put("gitConfigName", "config");
    validRepoSpec.put("fileSource", "BRANCH");
    validRepoSpec.put("filePath", "path");
    validRepoSpec.put("sourceId", "id");
    validRepoOptions.put("gcpConfigId", "id");
    validRepoOptions.put("specSource", "REMOTE");
    validRepoOptions.put("repositorySpec", validRepoSpec);
    propertiesWithValidRemoteSpec.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithValidRemoteSpec.put("gcbOptions", validRepoOptions);

    // parameter
    Map<String, Object> propertiesWithValidTriggerSpec = new HashMap<>();
    Map<String, Object> validTriggerOptions = new HashMap<>();
    Map<String, Object> validTriggerSpec = new HashMap<>();
    validTriggerSpec.put("source", "TAG");
    validTriggerSpec.put("sourceId", "id");
    validTriggerSpec.put("name", "triggerName");
    validTriggerOptions.put("gcpConfigId", "id");
    validTriggerOptions.put("specSource", "TRIGGER");
    validTriggerOptions.put("triggerSpec", validTriggerSpec);
    propertiesWithValidTriggerSpec.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithValidTriggerSpec.put("gcbOptions", validTriggerOptions);

    // parameter
    Map<String, Object> propertiesWithInvalidExpression = new HashMap<>();
    Map<String, Object> triggerOptionsExpressions = new HashMap<>();
    Map<String, Object> triggerSpecExpressions = new HashMap<>();
    triggerSpecExpressions.put("source", "TAG");
    triggerSpecExpressions.put("sourceId", "id");
    triggerSpecExpressions.put("name", "${triggerName");
    triggerOptionsExpressions.put("gcpConfigId", "id");
    triggerOptionsExpressions.put("specSource", "TRIGGER");
    triggerOptionsExpressions.put("triggerSpec", triggerSpecExpressions);
    propertiesWithInvalidExpression.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithInvalidExpression.put("gcbOptions", triggerOptionsExpressions);

    // parameter
    Map<String, Object> propertiesWithInvalidTriggerSourceIdExpression = new HashMap<>();
    Map<String, Object> triggerOptionsSourceIdExpressions = new HashMap<>();
    Map<String, Object> triggerSpecSourceIdExpressions = new HashMap<>();
    triggerSpecSourceIdExpressions.put("source", "TAG");
    triggerSpecSourceIdExpressions.put("sourceId", "${id");
    triggerSpecSourceIdExpressions.put("name", "triggerName");
    triggerOptionsSourceIdExpressions.put("gcpConfigId", "id");
    triggerOptionsSourceIdExpressions.put("specSource", "TRIGGER");
    triggerOptionsSourceIdExpressions.put("triggerSpec", triggerSpecSourceIdExpressions);
    propertiesWithInvalidTriggerSourceIdExpression.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithInvalidTriggerSourceIdExpression.put("gcbOptions", triggerOptionsSourceIdExpressions);

    // parameter
    Map<String, Object> propertiesWithInvalidRemoteSpecNameExpression = new HashMap<>();
    Map<String, Object> invalidRepoExpressionOptions = new HashMap<>();
    Map<String, Object> invalidRepoSpecExpression = new HashMap<>();
    invalidRepoSpecExpression.put("gitConfigName", "config");
    invalidRepoSpecExpression.put("fileSource", "BRANCH");
    invalidRepoSpecExpression.put("filePath", "${path");
    invalidRepoSpecExpression.put("sourceId", "id");
    invalidRepoExpressionOptions.put("gcpConfigId", "id");
    invalidRepoExpressionOptions.put("specSource", "REMOTE");
    invalidRepoExpressionOptions.put("repositorySpec", invalidRepoSpecExpression);
    propertiesWithInvalidRemoteSpecNameExpression.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithInvalidRemoteSpecNameExpression.put("gcbOptions", invalidRepoExpressionOptions);

    // parameter
    Map<String, Object> propertiesWithInvalidRemoteSpecSourceIdExpression = new HashMap<>();
    Map<String, Object> invalidRepoSourceIdExpressionOptions = new HashMap<>();
    Map<String, Object> invalidRepoSpecSourceIdExpression = new HashMap<>();
    invalidRepoSpecSourceIdExpression.put("gitConfigName", "config");
    invalidRepoSpecSourceIdExpression.put("fileSource", "BRANCH");
    invalidRepoSpecSourceIdExpression.put("filePath", "path");
    invalidRepoSpecSourceIdExpression.put("sourceId", "${variables.id");
    invalidRepoSourceIdExpressionOptions.put("gcpConfigId", "id");
    invalidRepoSourceIdExpressionOptions.put("specSource", "REMOTE");
    invalidRepoSourceIdExpressionOptions.put("repositorySpec", invalidRepoSpecSourceIdExpression);
    propertiesWithInvalidRemoteSpecSourceIdExpression.put("templateExpressions", Collections.EMPTY_LIST);
    propertiesWithInvalidRemoteSpecSourceIdExpression.put("gcbOptions", invalidRepoSourceIdExpressionOptions);

    return Arrays.asList(new Object[][] {
        {StepYaml.builder().properties(propertiesWithoutGcbOptions).build(),
            "Google Cloud Build step is incomplete. Please, provide gcbOptions", IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithNullNonTemplatizedGcpConfig).build(),
            "\"gcpConfigName\" could not be empty or null. Please, provide gcpConfigName or add templateExpression",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithoutSpecSource).build(),
            "gcbOptions are incomplete. Please, provide specSource (INLINE, REMOTE, TRIGGER)",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithNullInlineSpec).build(),
            "\"inlineSpec\" could not be empty or null within INLINE specSource, please provide value",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithNullTriggerSpec).build(),
            "\"triggerSpec\" could not be empty or null within TRIGGER specSource, please provide value",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesTriggerSpecWithoutSource).build(),
            "\"source\" could not be empty or null. Please, provide value", IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesTriggerSpecWithoutName).build(),
            "\"name\" could not be empty or null. Please, provide value", IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesTriggerSpecWithoutSourceId).build(),
            "\"sourceId\" could not be empty or null. Please, provide value", IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithNullRepoSpec).build(),
            "\"repositorySpec\" could not be empty or null within REMOTE specSource, please provide value",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithNullGitConfig).build(),
            "\"gitConfigName\" could not be empty or null. Please, provide gitConfigName or add templateExpression",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithoutFileSource).build(),
            "\"fileSource\" could not be empty or null. Please, provide value", IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithoutFilePath).build(),
            "\"filePath\" could not be empty or null. Please, provide value", IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithoutSourceId).build(),
            "\"sourceId\" could not be empty or null. Please, provide value", IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithValidInlineSpec).build(), null, null},
        {StepYaml.builder().properties(propertiesWithValidRemoteSpec).build(), null, null},
        {StepYaml.builder().properties(propertiesWithValidTriggerSpec).build(), null, null},
        {StepYaml.builder().properties(propertiesWithInvalidExpression).build(),
            "Invalid expression for \"name\". Please, provide value or valid expression",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithInvalidTriggerSourceIdExpression).build(),
            "Invalid expression for \"sourceId\". Please, provide value or valid expression",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithInvalidRemoteSpecNameExpression).build(),
            "Invalid expression for \"filePath\". Please, provide value or valid expression",
            IncompleteStateException.class},
        {StepYaml.builder().properties(propertiesWithInvalidRemoteSpecSourceIdExpression).build(),
            "Invalid expression for \"sourceId\". Please, provide value or valid expression",
            IncompleteStateException.class}});
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldValidateStepYaml() {
    validator.validate(stepYaml);
  }
}
