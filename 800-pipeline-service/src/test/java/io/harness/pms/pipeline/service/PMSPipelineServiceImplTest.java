package io.harness.pms.pipeline.service;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.filters.FilterCreatorMergeService;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.steps.StepInfo;
import io.harness.pms.steps.StepMetaData;
import io.harness.repositories.pipeline.PMSPipelineRepository;
import io.harness.rule.Owner;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@Slf4j
@RunWith(PowerMockRunner.class)
@PrepareForTest({CommonStepInfo.class})
@PowerMockIgnore({"javax.security.*", "org.apache.http.conn.ssl.", "javax.net.ssl.", "javax.crypto.*"})
public class PMSPipelineServiceImplTest extends PipelineServiceTestBase {
  @Mock private PMSPipelineRepository pmsPipelineRepository;
  @Mock private FilterCreatorMergeService filterCreatorMergeService;
  @Mock private PmsSdkInstanceService pmsSdkInstanceService;

  @InjectMocks private PMSPipelineServiceImpl pmsPipelineService;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testGetSteps() {
    Map<String, List<StepInfo>> serviceInstanceNameToSupportedSteps = new HashMap<>();
    serviceInstanceNameToSupportedSteps.put("cd",
        Collections.singletonList(StepInfo.newBuilder()
                                      .setName("testStepCD")
                                      .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                                      .build()));
    serviceInstanceNameToSupportedSteps.put("cv",
        Collections.singletonList(StepInfo.newBuilder()
                                      .setName("testStepCV")
                                      .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                                      .build()));
    Mockito.when(pmsSdkInstanceService.getInstanceNameToSupportedSteps())
        .thenReturn(serviceInstanceNameToSupportedSteps);
    PowerMockito.mockStatic(CommonStepInfo.class);
    Mockito.when(CommonStepInfo.getCommonSteps()).thenReturn(new ArrayList<>());

    StepCategory stepCategory = pmsPipelineService.getSteps("cd", null);
    String expected =
        "StepCategory(name=Library, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCD)], stepCategories=[])]), StepCategory(name=cv, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCV)], stepCategories=[])])])])";

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
                .setStepMetaData(StepMetaData.newBuilder().addCategory("K8S").setFolderPath("Double/Single").build())
                .build()));
    serviceInstanceNameToSupportedSteps.put("cv",
        Collections.singletonList(StepInfo.newBuilder()
                                      .setName("testStepCV")
                                      .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Double/Single").build())
                                      .build()));
    Mockito.when(pmsSdkInstanceService.getInstanceNameToSupportedSteps())
        .thenReturn(serviceInstanceNameToSupportedSteps);
    PowerMockito.mockStatic(CommonStepInfo.class);
    Mockito.when(CommonStepInfo.getCommonSteps()).thenReturn(new ArrayList<>());

    StepCategory stepCategory = pmsPipelineService.getSteps("cd", "Terraform");
    String expected =
        "StepCategory(name=Library, stepsData=[], stepCategories=[StepCategory(name=cv, stepsData=[], stepCategories=[StepCategory(name=Double, stepsData=[], stepCategories=[StepCategory(name=Single, stepsData=[StepData(name=testStepCV)], stepCategories=[])])])])";
    assertThat(stepCategory.toString()).isEqualTo(expected);
  }
}