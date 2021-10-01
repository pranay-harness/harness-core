package io.harness.engine.retry;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.retry.RetryExecutionHelper;
import io.harness.engine.executions.retry.RetryGroup;
import io.harness.engine.executions.retry.RetryInfo;
import io.harness.engine.executions.retry.RetryStageInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class RetryExecuteHelperTest {
  @InjectMocks private RetryExecutionHelper retryExecuteHelper;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  private String readFile(String filename) {
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      return Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not read resource file: " + filename);
    }
  }

  List<RetryStageInfo> getFirstStageFailed() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(100L)
                         .build());
    return stageDetails;
  }

  private List<RetryStageInfo> getlastStageFailed() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .nextId("stage3")
                         .parentId("parent2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent3")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(300L)
                         .build());
    return stageDetails;
  }

  private List<RetryStageInfo> getFirstStageParallelAndFailed() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent1")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent1")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(300L)
                         .build());
    return stageDetails;
  }

  private List<RetryStageInfo> getlastStageParallelAndFailed() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent1")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent1")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(300L)
                         .build());

    stageDetails.add(RetryStageInfo.builder()
                         .name("stage4")
                         .identifier("stage4")
                         .parentId("parent2")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage5")
                         .identifier("stage5")
                         .parentId("parent2")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(500L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage6")
                         .identifier("stage6")
                         .parentId("parent2")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(600L)
                         .build());

    stageDetails.add(RetryStageInfo.builder()
                         .name("stage7")
                         .identifier("stage7")
                         .parentId("parent3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(700L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage8")
                         .identifier("stage8")
                         .parentId("parent3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(800L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage9")
                         .identifier("stage9")
                         .parentId("parent3")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(900L)
                         .build());

    return stageDetails;
  }

  private List<RetryStageInfo> getMixTypeStagesWithParallelFailed() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent2")
                         .nextId("stage3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent3")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(300L)
                         .build());

    stageDetails.add(RetryStageInfo.builder()
                         .name("stage4")
                         .identifier("stage4")
                         .parentId("parent4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage5")
                         .identifier("stage5")
                         .parentId("parent4")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(500L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage6")
                         .identifier("stage6")
                         .parentId("parent4")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(600L)
                         .build());

    return stageDetails;
  }

  private List<RetryStageInfo> getMixTypeStagesWithSeriesStageFailed() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent2")
                         .nextId("stage3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent3")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(300L)
                         .build());

    stageDetails.add(RetryStageInfo.builder()
                         .name("stage4")
                         .identifier("stage4")
                         .parentId("parent4")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage5")
                         .identifier("stage5")
                         .parentId("parent4")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(500L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage6")
                         .identifier("stage6")
                         .parentId("parent4")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(600L)
                         .build());

    stageDetails.add(RetryStageInfo.builder()
                         .name("stage7")
                         .identifier("stage7")
                         .parentId("parent7")
                         .nextId("stage8")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage8")
                         .identifier("stage8")
                         .parentId("parent8")
                         .nextId("stage9")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(500L)
                         .build());
    stageDetails.add(RetryStageInfo.builder()
                         .name("stage9")
                         .identifier("stage9")
                         .parentId("parent9")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(600L)
                         .build());
    return stageDetails;
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetStagesSeries() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();

    // passing empty stageDetails
    RetryInfo retryInfo = retryExecuteHelper.getRetryInfo(stageDetails);
    assertThat(retryInfo).isNotNull();
    assertThat(retryInfo.getGroups().size()).isEqualTo(0);

    // making first stage as empty
    stageDetails = getFirstStageFailed();
    retryInfo = retryExecuteHelper.getRetryInfo(stageDetails);
    assertThat(retryInfo).isNotNull();
    assertThat(retryInfo.getGroups().get(0).getInfo()).isEqualTo(stageDetails);

    // making the last stageFailed
    stageDetails = getlastStageFailed();
    retryInfo = retryExecuteHelper.getRetryInfo(stageDetails);
    assertThat(retryInfo).isNotNull();
    assertThat(retryInfo.getGroups().size()).isEqualTo(3);
    assertThat(retryInfo.getGroups().get(0).getInfo().get(0)).isEqualTo(stageDetails.get(0));
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetStagesParallel() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    RetryInfo retryInfo;

    // making first stage as parallel and failed
    stageDetails = getFirstStageParallelAndFailed();
    retryInfo = retryExecuteHelper.getRetryInfo(stageDetails);
    assertThat(retryInfo).isNotNull();
    List<RetryGroup> retryGroupList = retryInfo.getGroups();
    assertThat(retryGroupList.get(0).getInfo()).isEqualTo(stageDetails);

    // having more than once parallel stages. All stages in parallel
    stageDetails = getlastStageParallelAndFailed();
    retryInfo = retryExecuteHelper.getRetryInfo(stageDetails);
    assertThat(retryInfo).isNotNull();
    retryGroupList = retryInfo.getGroups();
    assertThat(retryGroupList.size()).isEqualTo(3);
    assertThat(retryGroupList.get(0).getInfo().size()).isEqualTo(3);
    assertThat(retryGroupList.get(0).getInfo().get(0).getIdentifier()).isEqualTo("stage1");
    assertThat(retryGroupList.get(1).getInfo().get(0).getIdentifier()).isEqualTo("stage4");
    assertThat(retryGroupList.get(2).getInfo().get(0).getIdentifier()).isEqualTo("stage7");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetStagesSeriesAndParallel() {
    List<RetryStageInfo> stageDetails = new ArrayList<>();
    RetryInfo retryInfo;

    // parallel step failed after getting success for stages in series
    stageDetails = getMixTypeStagesWithParallelFailed();
    retryInfo = retryExecuteHelper.getRetryInfo(stageDetails);
    assertThat(retryInfo).isNotNull();
    List<RetryGroup> retryGroupList = retryInfo.getGroups();
    assertThat(retryGroupList.size()).isEqualTo(4);
    assertThat(retryGroupList.get(0).getInfo().get(0).getIdentifier()).isEqualTo("stage1");
    assertThat(retryGroupList.get(2).getInfo().get(0).getIdentifier()).isEqualTo("stage3");
    assertThat(retryGroupList.get(3).getInfo().size()).isEqualTo(3);

    // series stage failed having few stages in parallel before
    stageDetails = getMixTypeStagesWithSeriesStageFailed();
    retryInfo = retryExecuteHelper.getRetryInfo(stageDetails);
    assertThat(retryInfo).isNotNull();
    retryGroupList = retryInfo.getGroups();
    assertThat(retryGroupList.size()).isEqualTo(7);
    assertThat(retryGroupList.get(0).getInfo().get(0).getIdentifier()).isEqualTo("stage1");
    assertThat(retryGroupList.get(2).getInfo().get(0).getIdentifier()).isEqualTo("stage3");
    assertThat(retryGroupList.get(3).getInfo().size()).isEqualTo(3);
    assertThat(retryGroupList.get(6).getInfo().get(0).getIdentifier()).isEqualTo("stage9");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testValidateRetry() {
    // empty and null yaml values
    assertThat(retryExecuteHelper.validateRetry("updatedYaml", "")).isEqualTo(false);
    assertThat(retryExecuteHelper.validateRetry(null, "originalYaml")).isEqualTo(false);

    // same yaml
    String updatedYamlFile = "retry-updated1.yaml";
    String updatedYaml = readFile(updatedYamlFile);

    String originalYamlFile = "retry-original1.yaml";
    String originalYaml = readFile(originalYamlFile);

    assertThat(retryExecuteHelper.validateRetry(updatedYaml, originalYaml)).isEqualTo(true);

    // updated the yaml - adding a stage
    // same yaml
    String updatedYamlFile2 = "retry-updated2.yaml";
    String updatedYaml2 = readFile(updatedYamlFile2);

    String originalYamlFile2 = "retry-original2.yaml";
    String originalYaml2 = readFile(originalYamlFile2);

    assertThat(retryExecuteHelper.validateRetry(updatedYaml2, originalYaml2)).isEqualTo(false);

    // added step in on of the stage and changed the name of the stage
    String updatedYamlFile3 = "retry-updated3.yaml";
    String updatedYaml3 = readFile(updatedYamlFile3);

    String originalYamlFile3 = "retry-original3.yaml";
    String originalYaml3 = readFile(originalYamlFile3);

    assertThat(retryExecuteHelper.validateRetry(updatedYaml3, originalYaml3)).isEqualTo(true);

    // updated the identifier
    String updatedYamlFile4 = "retry-updated4.yaml";
    String updatedYaml4 = readFile(updatedYamlFile4);

    String originalYamlFile4 = "retry-original4.yaml";
    String originalYaml4 = readFile(originalYamlFile4);

    assertThat(retryExecuteHelper.validateRetry(updatedYaml4, originalYaml4)).isEqualTo(false);

    // shuffling of stages
    String updatedYamlFile5 = "retry-updated5.yaml";
    String updatedYaml5 = readFile(updatedYamlFile5);

    String originalYamlFile5 = "retry-original5.yaml";
    String originalYaml5 = readFile(originalYamlFile5);

    assertThat(retryExecuteHelper.validateRetry(updatedYaml5, originalYaml5)).isEqualTo(false);

    // adding the stage in parallel
    String updatedYamlFile6 = "retry-updated6.yaml";
    String updatedYaml6 = readFile(updatedYamlFile6);

    String originalYamlFile6 = "retry-original6.yaml";
    String originalYaml6 = readFile(originalYamlFile6);

    assertThat(retryExecuteHelper.validateRetry(updatedYaml6, originalYaml6)).isEqualTo(false);

    // shuffling of parallel stages
    String updatedYamlFile7 = "retry-updated7.yaml";
    String updatedYaml7 = readFile(updatedYamlFile7);

    String originalYamlFile7 = "retry-original7.yaml";
    String originalYaml7 = readFile(originalYamlFile7);

    assertThat(retryExecuteHelper.validateRetry(updatedYaml7, originalYaml7)).isEqualTo(false);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testRetryProcessedYaml() throws IOException {
    String previousYamlFile = "retry-processedYamlPrevious1.yaml";
    String previousYaml = readFile(previousYamlFile);
    String currentYamlFile = "retry-processedYamlCurrent1.yaml";
    String currentYaml = readFile(currentYamlFile);
    String resultYamlFile = "retry-processedYamlResult1.yaml";
    String resultYaml = readFile(resultYamlFile);
    List<String> uuidOfSkipNode = new ArrayList<>();
    String replacedProcessedYaml = retryExecuteHelper.retryProcessedYaml(
        previousYaml, currentYaml, Collections.singletonList("stage2"), uuidOfSkipNode);
    assertThat(replacedProcessedYaml).isEqualTo(resultYaml);

    // resuming from the first stage
    resultYamlFile = "retry-processedYamlResultFirstStageFailed1.yaml";
    resultYaml = readFile(resultYamlFile);
    replacedProcessedYaml = retryExecuteHelper.retryProcessedYaml(
        previousYaml, currentYaml, Collections.singletonList("stage1"), new ArrayList<>());
    assertThat(replacedProcessedYaml).isEqualTo(resultYaml);

    // failing a single stage which is ahead of some parallel stages
    String previousGoldenYamlFile = "retry-processedYamlPreviousGolden.yaml";
    String previousGoldenYaml = readFile(previousGoldenYamlFile);
    String currentGoldenYamlFile = "retry-processedYamlCurrentGolden.yaml";
    String currentGoldenYaml = readFile(currentGoldenYamlFile);
    String resultProcessedFile = "retry-processedYamlResultGolden1.yaml";
    String resultProcessedYaml = readFile(resultProcessedFile);
    replacedProcessedYaml = retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Collections.singletonList("stage7"), new ArrayList<>());
    assertThat(replacedProcessedYaml).isEqualTo(yamlToJsonString(resultProcessedYaml));

    // failing single stages from parallel groups
    resultProcessedFile = "retry-processedYamlResultSingleStageFailedInParallelStages.yaml";
    resultProcessedYaml = readFile(resultProcessedFile);
    replacedProcessedYaml = retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Collections.singletonList("stage9"), new ArrayList<>());
    assertThat(replacedProcessedYaml).isEqualTo(yamlToJsonString(resultProcessedYaml));

    // failing multiple stage failure in parallel group
    resultProcessedFile = "retry-processedYamlResultMultipleStageFailedInParallelStages.yaml";
    resultProcessedYaml = readFile(resultProcessedFile);
    replacedProcessedYaml = retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Arrays.asList("stage3", "stage5"), new ArrayList<>());
    assertThat(replacedProcessedYaml).isEqualTo(yamlToJsonString(resultProcessedYaml));

    // selecting all stages in parallel group
    resultProcessedFile = "retry-processedYamlResultAllStageFailedInParallelStages.yaml";
    resultProcessedYaml = readFile(resultProcessedFile);
    replacedProcessedYaml = retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Arrays.asList("stage3", "stage4", "stage5"), new ArrayList<>());
    assertThat(replacedProcessedYaml).isEqualTo(yamlToJsonString(resultProcessedYaml));
  }

  private String yamlToJsonString(String resultProcessedYaml) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper.readTree(resultProcessedYaml).toString();
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testIsFailedStatus() {
    assertThat(retryExecuteHelper.isFailedStatus(ExecutionStatus.EXPIRED)).isEqualTo(true);
    assertThat(retryExecuteHelper.isFailedStatus(ExecutionStatus.ABORTED)).isEqualTo(true);
    assertThat(retryExecuteHelper.isFailedStatus(ExecutionStatus.FAILED)).isEqualTo(true);
    assertThat(retryExecuteHelper.isFailedStatus(ExecutionStatus.APPROVAL_REJECTED)).isEqualTo(true);
    assertThat(retryExecuteHelper.isFailedStatus(ExecutionStatus.APPROVALREJECTED)).isEqualTo(true);

    assertThat(retryExecuteHelper.isFailedStatus(ExecutionStatus.SUCCESS)).isEqualTo(false);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testFetchOnlyFailedStages() {
    List<RetryStageInfo> retryStageInfos = new ArrayList<>();
    List<String> stageIdentifier = new ArrayList<>();
    assertThatThrownBy(() -> retryExecuteHelper.fetchOnlyFailedStages(retryStageInfos, stageIdentifier))
        .isInstanceOf(InvalidRequestException.class);

    // testing caching of exception
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage1").build());
    stageIdentifier.add("stage2");
    assertThatThrownBy(() -> retryExecuteHelper.fetchOnlyFailedStages(retryStageInfos, stageIdentifier))
        .isInstanceOf(InvalidRequestException.class);

    stageIdentifier.clear();
    retryStageInfos.clear();

    // testing whole valid status
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage1").status(ExecutionStatus.SUCCESS).build());
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage2").status(ExecutionStatus.ABORTED).build());
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage3").status(ExecutionStatus.IGNOREFAILED).build());
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage4").status(ExecutionStatus.FAILED).build());
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage5").status(ExecutionStatus.EXPIRED).build());
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage6").status(ExecutionStatus.APPROVALREJECTED).build());
    retryStageInfos.add(RetryStageInfo.builder().identifier("stage7").status(ExecutionStatus.APPROVALREJECTED).build());

    stageIdentifier.add("stage1");
    stageIdentifier.add("stage2");
    stageIdentifier.add("stage3");
    stageIdentifier.add("stage4");
    stageIdentifier.add("stage5");
    stageIdentifier.add("stage6");
    stageIdentifier.add("stage7");

    List<String> onlyFailedStageIdentifier = retryExecuteHelper.fetchOnlyFailedStages(retryStageInfos, stageIdentifier);
    assertThat(onlyFailedStageIdentifier.size()).isEqualTo(5);
    assertThat(onlyFailedStageIdentifier).contains("stage2");
    assertThat(onlyFailedStageIdentifier).contains("stage4");
    assertThat(onlyFailedStageIdentifier).contains("stage5");
    assertThat(onlyFailedStageIdentifier).contains("stage6");
    assertThat(onlyFailedStageIdentifier).contains("stage7");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testFetchUuidOfNonRetryStages() throws IOException {
    String previousYamlFile = "retry-processedYamlPrevious1.yaml";
    String previousYaml = readFile(previousYamlFile);
    String currentYamlFile = "retry-processedYamlCurrent1.yaml";
    String currentYaml = readFile(currentYamlFile);
    List<String> uuidOfSkipNode = new ArrayList<>();
    retryExecuteHelper.retryProcessedYaml(
        previousYaml, currentYaml, Collections.singletonList("stage2"), uuidOfSkipNode);

    // resuming from the first stage
    uuidOfSkipNode.clear();
    retryExecuteHelper.retryProcessedYaml(
        previousYaml, currentYaml, Collections.singletonList("stage1"), uuidOfSkipNode);
    assertThat(uuidOfSkipNode.size()).isEqualTo(0);

    // failing a single stage which is ahead of some parallel stages
    uuidOfSkipNode.clear();
    String previousGoldenYamlFile = "retry-processedYamlPreviousGolden.yaml";
    String previousGoldenYaml = readFile(previousGoldenYamlFile);
    String currentGoldenYamlFile = "retry-processedYamlCurrentGolden.yaml";
    String currentGoldenYaml = readFile(currentGoldenYamlFile);
    retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Collections.singletonList("stage7"), uuidOfSkipNode);
    assertThat(uuidOfSkipNode.size()).isEqualTo(6);
    assertThat(uuidOfSkipNode.get(0)).isEqualTo("oldUuid1");
    assertThat(uuidOfSkipNode.get(1)).isEqualTo("oldUuid2");
    assertThat(uuidOfSkipNode.get(2)).isEqualTo("oldUuid3");
    assertThat(uuidOfSkipNode.get(3)).isEqualTo("oldUuid4");
    assertThat(uuidOfSkipNode.get(4)).isEqualTo("oldUuid5");
    assertThat(uuidOfSkipNode.get(5)).isEqualTo("oldUuid6");

    // failing single stages from parallel groups
    uuidOfSkipNode.clear();
    retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Collections.singletonList("stage9"), uuidOfSkipNode);
    assertThat(uuidOfSkipNode.size()).isEqualTo(8);
    assertThat(uuidOfSkipNode.get(0)).isEqualTo("oldUuid1");
    assertThat(uuidOfSkipNode.get(1)).isEqualTo("oldUuid2");
    assertThat(uuidOfSkipNode.get(2)).isEqualTo("oldUuid3");
    assertThat(uuidOfSkipNode.get(3)).isEqualTo("oldUuid4");
    assertThat(uuidOfSkipNode.get(4)).isEqualTo("oldUuid5");
    assertThat(uuidOfSkipNode.get(5)).isEqualTo("oldUuid6");
    assertThat(uuidOfSkipNode.get(6)).isEqualTo("oldUuid7");
    assertThat(uuidOfSkipNode.get(7)).isEqualTo("oldUuid8");

    // failing multiple stage failure in parallel group
    uuidOfSkipNode.clear();
    retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Arrays.asList("stage3", "stage5"), uuidOfSkipNode);
    assertThat(uuidOfSkipNode.size()).isEqualTo(3);
    assertThat(uuidOfSkipNode.get(0)).isEqualTo("oldUuid1");
    assertThat(uuidOfSkipNode.get(1)).isEqualTo("oldUuid2");
    assertThat(uuidOfSkipNode.get(2)).isEqualTo("oldUuid4");

    // selecting all stages in parallel group
    uuidOfSkipNode.clear();
    retryExecuteHelper.retryProcessedYaml(
        previousGoldenYaml, currentGoldenYaml, Arrays.asList("stage3", "stage4", "stage5"), uuidOfSkipNode);
    assertThat(uuidOfSkipNode.size()).isEqualTo(2);
    assertThat(uuidOfSkipNode.get(0)).isEqualTo("oldUuid1");
    assertThat(uuidOfSkipNode.get(1)).isEqualTo("oldUuid2");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testTransformPlan() {
    StepType TEST_STEP_TYPE =
        StepType.newBuilder().setType("TEST_STEP_PLAN").setStepCategory(StepCategory.STEP).build();
    String uuid = "uuid1";
    PlanNode planNode =
        PlanNode.builder()
            .name("Test Node")
            .uuid(uuid)
            .identifier("test")
            .stepType(TEST_STEP_TYPE)
            .adviserObtainment(
                AdviserObtainment.newBuilder().setType(AdviserType.newBuilder().setType("NEXT_STEP").build()).build())
            .build();
    retryExecuteHelper.transformPlan(
        Plan.builder().planNodes(Collections.singleton(planNode)).build(), Collections.singletonList(uuid), "abc");
  }
}
