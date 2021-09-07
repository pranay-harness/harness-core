package io.harness.pms.plan;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.resume.ResumeStageInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.pipeline.mappers.NodeExecutionToExecutioNodeMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineServiceImpl;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.plan.execution.PipelineExecuteHelper;
import io.harness.pms.plan.execution.ResumeGroup;
import io.harness.pms.plan.execution.ResumeInfo;
import io.harness.pms.plan.execution.service.PMSExecutionService;
import io.harness.repositories.pipeline.PMSPipelineRepositoryCustomImpl;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineExecuteHelperTest {
  @Mock PMSPipelineService pmsPipelineService;
  @Mock PMSExecutionService pmsExecutionService;
  @Mock PMSYamlSchemaService pmsYamlSchemaService;
  @Mock NodeExecutionService nodeExecutionService;
  @Mock NodeExecutionToExecutioNodeMapper nodeExecutionToExecutioNodeMapper;
  @Mock AccessControlClient accessControlClient;
  @Mock PmsGitSyncHelper pmsGitSyncHelper;

  @InjectMocks private PMSPipelineServiceImpl pmsPipelineServiceImpl;
  @InjectMocks private PipelineExecuteHelper pipelineExecuteHelper;
  @InjectMocks private PMSPipelineRepositoryCustomImpl pmsPipelineRepositoryCustom;

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

  List<ResumeStageInfo> getFirstStageFailed() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(100L)
                         .build());
    return stageDetails;
  }

  private List<ResumeStageInfo> getlastStageFailed() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .nextId("stage3")
                         .parentId("parent2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent3")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(300L)
                         .build());
    return stageDetails;
  }

  private List<ResumeStageInfo> getFirstStageParallelAndFailed() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent1")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent1")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(300L)
                         .build());
    return stageDetails;
  }

  private List<ResumeStageInfo> getlastStageParallelAndFailed() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent1")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent1")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(300L)
                         .build());

    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage4")
                         .identifier("stage4")
                         .parentId("parent2")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage5")
                         .identifier("stage5")
                         .parentId("parent2")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(500L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage6")
                         .identifier("stage6")
                         .parentId("parent2")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(600L)
                         .build());

    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage7")
                         .identifier("stage7")
                         .parentId("parent3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(700L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage8")
                         .identifier("stage8")
                         .parentId("parent3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(800L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage9")
                         .identifier("stage9")
                         .parentId("parent3")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(900L)
                         .build());

    return stageDetails;
  }

  private List<ResumeStageInfo> getMixTypeStagesWithParallelFailed() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent2")
                         .nextId("stage3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent3")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(300L)
                         .build());

    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage4")
                         .identifier("stage4")
                         .parentId("parent4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage5")
                         .identifier("stage5")
                         .parentId("parent4")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(500L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage6")
                         .identifier("stage6")
                         .parentId("parent4")
                         .status(ExecutionStatus.FAILED)
                         .createdAt(600L)
                         .build());

    return stageDetails;
  }

  private List<ResumeStageInfo> getMixTypeStagesWithSeriesStageFailed() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage1")
                         .identifier("stage1")
                         .parentId("parent1")
                         .nextId("stage2")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(100L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage2")
                         .identifier("stage2")
                         .parentId("parent2")
                         .nextId("stage3")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(200L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage3")
                         .identifier("stage3")
                         .parentId("parent3")
                         .nextId("stage4")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(300L)
                         .build());

    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage4")
                         .identifier("stage4")
                         .parentId("parent4")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage5")
                         .identifier("stage5")
                         .parentId("parent4")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(500L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage6")
                         .identifier("stage6")
                         .parentId("parent4")
                         .nextId("stage7")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(600L)
                         .build());

    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage7")
                         .identifier("stage7")
                         .parentId("parent7")
                         .nextId("stage8")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(400L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
                         .name("stage8")
                         .identifier("stage8")
                         .parentId("parent8")
                         .nextId("stage9")
                         .status(ExecutionStatus.SUCCESS)
                         .createdAt(500L)
                         .build());
    stageDetails.add(ResumeStageInfo.builder()
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
    List<ResumeStageInfo> stageDetails = new ArrayList<>();

    // passing empty stageDetails
    ResumeInfo resumeInfo = pipelineExecuteHelper.getResumeInfo(stageDetails);
    assertThat(resumeInfo).isNotNull();
    assertThat(resumeInfo.getGroups().size()).isEqualTo(0);

    // making first stage as empty
    stageDetails = getFirstStageFailed();
    resumeInfo = pipelineExecuteHelper.getResumeInfo(stageDetails);
    assertThat(resumeInfo).isNotNull();
    assertThat(resumeInfo.getGroups().get(0).getInfo()).isEqualTo(stageDetails);

    // making the last stageFailed
    stageDetails = getlastStageFailed();
    resumeInfo = pipelineExecuteHelper.getResumeInfo(stageDetails);
    assertThat(resumeInfo).isNotNull();
    assertThat(resumeInfo.getGroups().size()).isEqualTo(3);
    assertThat(resumeInfo.getGroups().get(0).getInfo().get(0)).isEqualTo(stageDetails.get(0));
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetStagesParallel() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    ResumeInfo resumeInfo;

    // making first stage as parallel and failed
    stageDetails = getFirstStageParallelAndFailed();
    resumeInfo = pipelineExecuteHelper.getResumeInfo(stageDetails);
    assertThat(resumeInfo).isNotNull();
    List<ResumeGroup> resumeGroupList = resumeInfo.getGroups();
    assertThat(resumeGroupList.get(0).getInfo()).isEqualTo(stageDetails);

    // having more than once parallel stages. All stages in parallel
    stageDetails = getlastStageParallelAndFailed();
    resumeInfo = pipelineExecuteHelper.getResumeInfo(stageDetails);
    assertThat(resumeInfo).isNotNull();
    resumeGroupList = resumeInfo.getGroups();
    assertThat(resumeGroupList.size()).isEqualTo(3);
    assertThat(resumeGroupList.get(0).getInfo().size()).isEqualTo(3);
    assertThat(resumeGroupList.get(0).getInfo().get(0).getIdentifier()).isEqualTo("stage1");
    assertThat(resumeGroupList.get(1).getInfo().get(0).getIdentifier()).isEqualTo("stage4");
    assertThat(resumeGroupList.get(2).getInfo().get(0).getIdentifier()).isEqualTo("stage7");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetStagesSeriesAndParallel() {
    List<ResumeStageInfo> stageDetails = new ArrayList<>();
    ResumeInfo resumeInfo;

    // parallel step failed after getting success for stages in series
    stageDetails = getMixTypeStagesWithParallelFailed();
    resumeInfo = pipelineExecuteHelper.getResumeInfo(stageDetails);
    assertThat(resumeInfo).isNotNull();
    List<ResumeGroup> resumeGroupList = resumeInfo.getGroups();
    assertThat(resumeGroupList.size()).isEqualTo(4);
    assertThat(resumeGroupList.get(0).getInfo().get(0).getIdentifier()).isEqualTo("stage1");
    assertThat(resumeGroupList.get(2).getInfo().get(0).getIdentifier()).isEqualTo("stage3");
    assertThat(resumeGroupList.get(3).getInfo().size()).isEqualTo(3);

    // series stage failed having few stages in parallel before
    stageDetails = getMixTypeStagesWithSeriesStageFailed();
    resumeInfo = pipelineExecuteHelper.getResumeInfo(stageDetails);
    assertThat(resumeInfo).isNotNull();
    resumeGroupList = resumeInfo.getGroups();
    assertThat(resumeGroupList.size()).isEqualTo(7);
    assertThat(resumeGroupList.get(0).getInfo().get(0).getIdentifier()).isEqualTo("stage1");
    assertThat(resumeGroupList.get(2).getInfo().get(0).getIdentifier()).isEqualTo("stage3");
    assertThat(resumeGroupList.get(3).getInfo().size()).isEqualTo(3);
    assertThat(resumeGroupList.get(6).getInfo().get(0).getIdentifier()).isEqualTo("stage9");
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testValidateResume() {
    // empty and null yaml values
    assertThat(pipelineExecuteHelper.validateResume("updatedYaml", "")).isEqualTo(false);
    assertThat(pipelineExecuteHelper.validateResume(null, "originalYaml")).isEqualTo(false);

    // same yaml
    String updatedYamlFile = "resume-updated1.yaml";
    String updatedYaml = readFile(updatedYamlFile);

    String originalYamlFile = "resume-original1.yaml";
    String originalYaml = readFile(originalYamlFile);

    assertThat(pipelineExecuteHelper.validateResume(updatedYaml, originalYaml)).isEqualTo(true);

    // updated the yaml - adding a stage
    // same yaml
    String updatedYamlFile2 = "resume-updated2.yaml";
    String updatedYaml2 = readFile(updatedYamlFile2);

    String originalYamlFile2 = "resume-original2.yaml";
    String originalYaml2 = readFile(originalYamlFile2);

    assertThat(pipelineExecuteHelper.validateResume(updatedYaml2, originalYaml2)).isEqualTo(false);

    // added step in on of the stage and changed the name of the stage
    String updatedYamlFile3 = "resume-updated3.yaml";
    String updatedYaml3 = readFile(updatedYamlFile3);

    String originalYamlFile3 = "resume-original3.yaml";
    String originalYaml3 = readFile(originalYamlFile3);

    assertThat(pipelineExecuteHelper.validateResume(updatedYaml3, originalYaml3)).isEqualTo(true);

    // updated the identifier
    String updatedYamlFile4 = "resume-updated4.yaml";
    String updatedYaml4 = readFile(updatedYamlFile4);

    String originalYamlFile4 = "resume-original4.yaml";
    String originalYaml4 = readFile(originalYamlFile4);

    assertThat(pipelineExecuteHelper.validateResume(updatedYaml4, originalYaml4)).isEqualTo(false);

    // shuffling of stages
    String updatedYamlFile5 = "resume-updated5.yaml";
    String updatedYaml5 = readFile(updatedYamlFile5);

    String originalYamlFile5 = "resume-original5.yaml";
    String originalYaml5 = readFile(originalYamlFile5);

    assertThat(pipelineExecuteHelper.validateResume(updatedYaml5, originalYaml5)).isEqualTo(false);

    // adding the stage in parallel
    String updatedYamlFile6 = "resume-updated6.yaml";
    String updatedYaml6 = readFile(updatedYamlFile6);

    String originalYamlFile6 = "resume-original6.yaml";
    String originalYaml6 = readFile(originalYamlFile6);

    assertThat(pipelineExecuteHelper.validateResume(updatedYaml6, originalYaml6)).isEqualTo(false);

    // shuffling of parallel stages
    String updatedYamlFile7 = "resume-updated7.yaml";
    String updatedYaml7 = readFile(updatedYamlFile7);

    String originalYamlFile7 = "resume-original7.yaml";
    String originalYaml7 = readFile(originalYamlFile7);

    assertThat(pipelineExecuteHelper.validateResume(updatedYaml7, originalYaml7)).isEqualTo(false);
  }
}
