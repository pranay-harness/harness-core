package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAHIL;
import static io.harness.rule.OwnerRule.SAMARTH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.impl.OutboxServiceImpl;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepData;
import io.harness.pms.pipeline.StepPalleteInfo;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.repositories.pipeline.PMSPipelineRepository;
import io.harness.repositories.pipeline.PMSPipelineRepositoryCustomImpl;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceImplTest extends PipelineServiceTestBase {
  @Mock private PmsSdkInstanceService pmsSdkInstanceService;
  @Mock private PMSPipelineServiceStepHelper pmsPipelineServiceStepHelper;
  @Mock private PMSPipelineServiceHelper pmsPipelineServiceHelper;
  @Mock private PMSPipelineRepositoryCustomImpl pmsPipelineRepositoryCustom;
  @Mock private OutboxServiceImpl outboxService;
  @InjectMocks private PMSPipelineServiceImpl pmsPipelineService;
  @Inject private PMSPipelineRepository pmsPipelineRepository;
  StepCategory library;
  StepCategory cv;

  private final String accountId = RandomStringUtils.randomAlphanumeric(6);
  private final String ORG_IDENTIFIER = "orgId";
  private final String PROJ_IDENTIFIER = "projId";
  private final String PIPELINE_IDENTIFIER = "myPipeline";

  private String yaml;

  PipelineEntity pipelineEntity;
  PipelineEntity updatedPipelineEntity;
  OutboxEvent a = OutboxEvent.builder().build();

  @Before
  public void setUp() throws IOException {
    StepCategory testStepCD =
        StepCategory.builder()
            .name("Single")
            .stepsData(Collections.singletonList(StepData.builder().name("testStepCD").type("testStepCD").build()))
            .stepCategories(Collections.emptyList())
            .build();
    StepCategory libraryDouble = StepCategory.builder()
                                     .name("Double")
                                     .stepsData(Collections.emptyList())
                                     .stepCategories(Collections.singletonList(testStepCD))
                                     .build();
    List<StepCategory> list = new ArrayList<>();
    list.add(libraryDouble);
    library = StepCategory.builder().name("Library").stepsData(new ArrayList<>()).stepCategories(list).build();

    StepCategory testStepCV =
        StepCategory.builder()
            .name("Single")
            .stepsData(Collections.singletonList(StepData.builder().name("testStepCV").type("testStepCV").build()))
            .stepCategories(Collections.emptyList())
            .build();
    StepCategory libraryDoubleCV = StepCategory.builder()
                                       .name("Double")
                                       .stepsData(Collections.emptyList())
                                       .stepCategories(Collections.singletonList(testStepCV))
                                       .build();
    List<StepCategory> listCV = new ArrayList<>();
    listCV.add(libraryDoubleCV);
    cv = StepCategory.builder().name("cv").stepsData(new ArrayList<>()).stepCategories(listCV).build();

    ClassLoader classLoader = this.getClass().getClassLoader();
    String filename = "failure-strategy.yaml";
    yaml = Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);

    pipelineEntity = PipelineEntity.builder()
                         .accountId(accountId)
                         .orgIdentifier(ORG_IDENTIFIER)
                         .projectIdentifier(PROJ_IDENTIFIER)
                         .identifier(PIPELINE_IDENTIFIER)
                         .name(PIPELINE_IDENTIFIER)
                         .yaml(yaml)
                         .stageCount(1)
                         .stageName("qaStage")
                         .version(null)
                         .deleted(false)
                         .createdAt(System.currentTimeMillis())
                         .lastUpdatedAt(System.currentTimeMillis())
                         .build();

    updatedPipelineEntity = pipelineEntity.withStageCount(1).withStageNames(Collections.singletonList("qaStage"));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetSteps() {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps = new HashMap<>();
    serviceInstanceNameToSupportedSteps.put("cd",
        StepPalleteInfo.builder()
            .moduleName("cd")
            .stepTypes(Collections.singletonList(
                StepInfo.newBuilder()
                    .setName("testStepCD")
                    .setType("testStepCD")
                    .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                    .build()))
            .build());
    serviceInstanceNameToSupportedSteps.put("cv",
        StepPalleteInfo.builder()
            .moduleName("cv")
            .stepTypes(Collections.singletonList(
                StepInfo.newBuilder()
                    .setName("testStepCV")
                    .setType("testStepCV")
                    .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                    .build()))
            .build());

    Mockito.when(pmsSdkInstanceService.getModuleNameToStepPalleteInfo())
        .thenReturn(serviceInstanceNameToSupportedSteps);
    Mockito
        .when(pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
            null, serviceInstanceNameToSupportedSteps.get("cd").getStepTypes(), accountId))
        .thenReturn(library);
    Mockito
        .when(pmsPipelineServiceStepHelper.calculateStepsForCategory(
            "cv", serviceInstanceNameToSupportedSteps.get("cv").getStepTypes(), accountId))
        .thenReturn(cv);
    StepCategory stepCategory = pmsPipelineService.getSteps("cd", null, accountId);
    String expected =
        "StepCategory(name=Library, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCD, type=testStepCD)], stepCategories=[])]), StepCategory(name=cv, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCV, type=testStepCV)], stepCategories=[])])])])";

    assertThat(stepCategory.toString()).isEqualTo(expected);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetStepsWithCategory() {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps = new HashMap<>();
    serviceInstanceNameToSupportedSteps.put("cd",
        StepPalleteInfo.builder()
            .moduleName("cd")
            .stepTypes(Collections.singletonList(
                StepInfo.newBuilder()
                    .setName("testStepCD")
                    .setType("testStepCD")
                    .setStepMetaData(
                        StepMetaData.newBuilder().addCategory("K8S").setFolderPath("Double/Single").build())
                    .build()))
            .build());
    serviceInstanceNameToSupportedSteps.put("cv",
        StepPalleteInfo.builder()
            .moduleName("cv")
            .stepTypes(Collections.singletonList(
                StepInfo.newBuilder()
                    .setName("testStepCV")
                    .setType("testStepCV")
                    .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                    .build()))
            .build());

    Mockito.when(pmsSdkInstanceService.getModuleNameToStepPalleteInfo())
        .thenReturn(serviceInstanceNameToSupportedSteps);
    Mockito
        .when(pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
            "Terraform", serviceInstanceNameToSupportedSteps.get("cd").getStepTypes(), accountId))
        .thenReturn(StepCategory.builder()
                        .name("Library")
                        .stepsData(new ArrayList<>())
                        .stepCategories(new ArrayList<>())
                        .build());
    Mockito
        .when(pmsPipelineServiceStepHelper.calculateStepsForCategory(
            "cv", serviceInstanceNameToSupportedSteps.get("cv").getStepTypes(), accountId))
        .thenReturn(cv);

    StepCategory stepCategory = pmsPipelineService.getSteps("cd", "Terraform", accountId);
    String expected =
        "StepCategory(name=Library, stepsData=[], stepCategories=[StepCategory(name=cv, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCV, type=testStepCV)], stepCategories=[])])])])";
    assertThat(stepCategory.toString()).isEqualTo(expected);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testFormCriteria() {
    Criteria form =
        pmsPipelineService.formCriteria(accountId, ORG_IDENTIFIER, PROJ_IDENTIFIER, null, null, false, null, null);

    assertThat(form.getCriteriaObject().get("accountId").toString().contentEquals(accountId)).isEqualTo(true);
    assertThat(form.getCriteriaObject().get("orgIdentifier").toString().contentEquals(ORG_IDENTIFIER)).isEqualTo(true);
    assertThat(form.getCriteriaObject().get("projectIdentifier").toString().contentEquals(PROJ_IDENTIFIER))
        .isEqualTo(true);
    assertThat(form.getCriteriaObject().containsKey("status")).isEqualTo(false);
  }

  @Test
  @Owner(developers = SAMARTH)
  @Category(UnitTests.class)
  public void testFormCriteriaWithActualData() throws IOException {
    on(pmsPipelineService).set("pmsPipelineRepository", pmsPipelineRepository);
    doReturn(a).when(outboxService).save(any());
    doReturn(updatedPipelineEntity).when(pmsPipelineServiceHelper).updatePipelineInfo(pipelineEntity);

    pmsPipelineService.create(pipelineEntity);

    Criteria criteria =
        pmsPipelineService.formCriteria(accountId, ORG_IDENTIFIER, PROJ_IDENTIFIER, null, null, false, "cd", "my");

    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.createdAt));

    List<PipelineEntity> list =
        pmsPipelineService.list(criteria, pageable, accountId, ORG_IDENTIFIER, PROJ_IDENTIFIER).getContent();

    assertThat(list.size()).isEqualTo(1);
    PipelineEntity queriedPipelineEntity = list.get(0);
    assertThat(queriedPipelineEntity.getAccountId()).isEqualTo(updatedPipelineEntity.getAccountId());
    assertThat(queriedPipelineEntity.getOrgIdentifier()).isEqualTo(updatedPipelineEntity.getOrgIdentifier());
    assertThat(queriedPipelineEntity.getIdentifier()).isEqualTo(updatedPipelineEntity.getIdentifier());
    assertThat(queriedPipelineEntity.getName()).isEqualTo(updatedPipelineEntity.getName());
    assertThat(queriedPipelineEntity.getYaml()).isEqualTo(updatedPipelineEntity.getYaml());
    assertThat(queriedPipelineEntity.getStageCount()).isEqualTo(updatedPipelineEntity.getStageCount());
    assertThat(queriedPipelineEntity.getStageNames()).isEqualTo(updatedPipelineEntity.getStageNames());
  }
}