package io.harness.ci.plan.creator;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.StepMetadata;
import io.harness.beans.steps.StepSpecTypeConstants;
import io.harness.ci.creator.variables.CIStageVariableCreator;
import io.harness.ci.creator.variables.CIStepVariableCreator;
import io.harness.ci.creator.variables.RunStepVariableCreator;
import io.harness.ci.plan.creator.filter.CIStageFilterJsonCreator;
import io.harness.ci.plan.creator.stage.IntegrationStagePMSPlanCreator;
import io.harness.ci.plan.creator.step.CIPMSStepFilterJsonCreator;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreator;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.sdk.core.pipeline.variables.ExecutionVariableCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.VariableCreator;
import io.harness.pms.utils.InjectorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Singleton
@OwnedBy(HarnessTeam.CI)
public class CIPipelineServiceInfoProvider implements PipelineServiceInfoProvider {
  @Inject InjectorUtils injectorUtils;

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new LinkedList<>();
    planCreators.add(new IntegrationStagePMSPlanCreator());
    planCreators.add(new CIPMSStepPlanCreator());
    injectorUtils.injectMembers(planCreators);
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new CIStageFilterJsonCreator());
    filterJsonCreators.add(new CIPMSStepFilterJsonCreator());
    injectorUtils.injectMembers(filterJsonCreators);

    return filterJsonCreators;
  }

  @Override
  public List<VariableCreator> getVariableCreators() {
    List<VariableCreator> variableCreators = new ArrayList<>();
    variableCreators.add(new CIStageVariableCreator());
    variableCreators.add(new ExecutionVariableCreator());
    variableCreators.add(new CIStepVariableCreator());
    variableCreators.add(new RunStepVariableCreator());
    return variableCreators;
  }

  @Override
  public List<StepInfo> getStepInfo() {
    StepInfo runStepInfo = getStepInfo("Run", StepSpecTypeConstants.RUN, Collections.singletonList("Build"));
    StepInfo runTestsStepInfo =
        getStepInfo("Run Tests", StepSpecTypeConstants.RUN_TEST, Collections.singletonList("Build"));
    StepInfo pluginStepInfo = getStepInfo("Plugin", StepSpecTypeConstants.PLUGIN, Collections.singletonList("Build"));

    StepInfo saveCacheToGCS =
        getStepInfo("Save Cache to GCS", StepSpecTypeConstants.SAVE_CACHE_GCS, Collections.singletonList("Build"));
    StepInfo restoreCacheFromGCS = getStepInfo(
        "Restore Cache From GCS", StepSpecTypeConstants.RESTORE_CACHE_GCS, Collections.singletonList("Build"));
    StepInfo restoreCacheFromS3 = getStepInfo(
        "Restore Cache From S3", StepSpecTypeConstants.RESTORE_CACHE_S3, Collections.singletonList("Build"));
    StepInfo saveCacheToS3 =
        getStepInfo("Save Cache to S3", StepSpecTypeConstants.SAVE_CACHE_S3, Collections.singletonList("Build"));

    StepInfo uploadToGCS =
        getStepInfo("Upload Artifacts to GCS", StepSpecTypeConstants.GCS_UPLOAD, Collections.singletonList("Build"));
    StepInfo uploadToS3 =
        getStepInfo("Upload Artifacts to S3", StepSpecTypeConstants.S3_UPLOAD, Collections.singletonList("Build"));
    StepInfo uploadArtifactsToJfrogBuild = getStepInfo("Upload Artifacts to JFrog Artifactory",
        StepSpecTypeConstants.ARTIFACTORY_UPLOAD, Arrays.asList("Artifacts", "Build"));

    StepInfo dockerPushBuild = getStepInfo("Build and Push an image to Docker Registry",
        StepSpecTypeConstants.BUILD_AND_PUSH_DOCKER_REGISTRY, Arrays.asList("Artifacts", "Build"));
    StepInfo gcrPushBuilds = getStepInfo(
        "Build and Push to GCR", StepSpecTypeConstants.BUILD_AND_PUSH_GCR, Arrays.asList("Artifacts", "Build"));
    StepInfo ecrPushBuilds = getStepInfo(
        "Build and Push to ECR", StepSpecTypeConstants.BUILD_AND_PUSH_ECR, Arrays.asList("Artifacts", "Build"));

    List<StepInfo> stepInfos = new ArrayList<>();

    stepInfos.add(runStepInfo);
    stepInfos.add(uploadToGCS);
    stepInfos.add(ecrPushBuilds);
    stepInfos.add(uploadToS3);
    stepInfos.add(gcrPushBuilds);
    stepInfos.add(restoreCacheFromGCS);
    stepInfos.add(runTestsStepInfo);
    stepInfos.add(pluginStepInfo);
    stepInfos.add(restoreCacheFromS3);
    stepInfos.add(dockerPushBuild);
    stepInfos.add(uploadArtifactsToJfrogBuild);
    stepInfos.add(saveCacheToGCS);
    stepInfos.add(saveCacheToS3);

    return stepInfos;
  }

  private StepInfo getStepInfo(String stepName, String stepType, List<String> folderPaths) {
    StepMetaData.Builder stepMetadataBuilder = StepMetaData.newBuilder();
    if (isNotEmpty(folderPaths)) {
      stepMetadataBuilder.addAllFolderPaths(folderPaths);
    }
    return StepInfo.newBuilder()
        .setName(stepName)
        .setType(stepType)
        .setStepMetaData(stepMetadataBuilder.build())
        .build();
  }
}
