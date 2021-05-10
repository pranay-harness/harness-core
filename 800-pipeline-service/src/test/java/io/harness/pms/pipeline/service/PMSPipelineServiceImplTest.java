package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepData;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceImplTest extends PipelineServiceTestBase {
  @Mock private PmsSdkInstanceService pmsSdkInstanceService;
  @Mock private PMSPipelineServiceStepHelper pmsPipelineServiceStepHelper;
  @InjectMocks private PMSPipelineServiceImpl pmsPipelineService;
  private final String accountId = "accountId";
  StepCategory library;

  @Before
  public void setUp() {
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
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetSteps() {
    Map<String, List<StepInfo>> serviceInstanceNameToSupportedSteps = new HashMap<>();
    serviceInstanceNameToSupportedSteps.put("cd",
        Collections.singletonList(StepInfo.newBuilder()
                                      .setName("testStepCD")
                                      .setType("testStepCD")
                                      .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                                      .build()));
    serviceInstanceNameToSupportedSteps.put("cv",
        Collections.singletonList(StepInfo.newBuilder()
                                      .setName("testStepCV")
                                      .setType("testStepCV")
                                      .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                                      .build()));
    Mockito.when(pmsSdkInstanceService.getInstanceNameToSupportedSteps())
        .thenReturn(serviceInstanceNameToSupportedSteps);
    Mockito
        .when(pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
            null, serviceInstanceNameToSupportedSteps.get("cd"), accountId))
        .thenReturn(library);

    StepCategory stepCategory = pmsPipelineService.getSteps("cd", null, accountId);
    String expected =
        "StepCategory(name=Library, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCD, type=testStepCD)], stepCategories=[])]), StepCategory(name=cv, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCV, type=testStepCV)], stepCategories=[])])])])";

    assertThat(stepCategory.toString()).isEqualTo(expected);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetStepsWithCategory() {
    Map<String, List<StepInfo>> serviceInstanceNameToSupportedSteps = new HashMap<>();
    serviceInstanceNameToSupportedSteps.put("cd",
        Collections.singletonList(
            StepInfo.newBuilder()
                .setName("testStepCD")
                .setType("testStepCD")
                .setStepMetaData(StepMetaData.newBuilder().addCategory("K8S").setFolderPath("Double/Single").build())
                .build()));
    serviceInstanceNameToSupportedSteps.put("cv",
        Collections.singletonList(StepInfo.newBuilder()
                                      .setName("testStepCV")
                                      .setType("testStepCV")
                                      .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                                      .build()));
    Mockito.when(pmsSdkInstanceService.getInstanceNameToSupportedSteps())
        .thenReturn(serviceInstanceNameToSupportedSteps);
    Mockito
        .when(pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
            "Terraform", serviceInstanceNameToSupportedSteps.get("cd"), accountId))
        .thenReturn(StepCategory.builder()
                        .name("Library")
                        .stepsData(new ArrayList<>())
                        .stepCategories(new ArrayList<>())
                        .build());

    StepCategory stepCategory = pmsPipelineService.getSteps("cd", "Terraform", accountId);
    String expected =
        "StepCategory(name=Library, stepsData=[], stepCategories=[StepCategory(name=cv, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCV, type=testStepCV)], stepCategories=[])])])])";
    assertThat(stepCategory.toString()).isEqualTo(expected);
  }
}