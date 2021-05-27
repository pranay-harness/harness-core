package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.MergeHelper.createTemplateFromPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.getPipelineComponent;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSets;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.inputset.helpers.MergeHelper;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetElementMapper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PIPELINE)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ValidateAndMergeHelper {
  private final PMSPipelineService pmsPipelineService;
  private final PMSInputSetService pmsInputSetService;

  public InputSetErrorWrapperDTOPMS validateInputSet(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String yaml, String pipelineBranch, String pipelineRepoID) {
    String identifier = PMSInputSetElementMapper.getStringField(yaml, "identifier", "inputSet");
    if (EmptyPredicate.isEmpty(identifier)) {
      throw new InvalidRequestException("Identifier cannot be empty");
    }
    confirmPipelineIdentifier(yaml, pipelineIdentifier);

    String pipelineYaml = getPipelineYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID);

    try {
      return MergeHelper.getErrorMap(pipelineYaml, yaml);
    } catch (IOException e) {
      throw new InvalidRequestException("Invalid input set yaml");
    }
  }

  private String getPipelineYaml(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String pipelineBranch, String pipelineRepoID) {
    GitSyncBranchContext gitSyncBranchContext =
        GitSyncBranchContext.builder()
            .gitBranchInfo(GitEntityInfo.builder().branch(pipelineBranch).yamlGitConfigId(pipelineRepoID).build())
            .build();

    String pipelineYaml;
    try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(gitSyncBranchContext, false)) {
      Optional<PipelineEntity> pipelineEntity =
          pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
      if (pipelineEntity.isPresent()) {
        pipelineYaml = pipelineEntity.get().getYaml();
      } else {
        throw new InvalidRequestException("Pipeline does not exist");
      }
    }
    return pipelineYaml;
  }

  public Map<String, String> validateOverlayInputSet(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, InputSetEntity entity) {
    if (EmptyPredicate.isEmpty(entity.getIdentifier())) {
      throw new InvalidRequestException("Identifier cannot be empty");
    }
    List<String> inputSetReferences = entity.getInputSetReferences();
    if (inputSetReferences.isEmpty()) {
      throw new InvalidRequestException("Input Set References can't be empty");
    }
    List<Optional<InputSetEntity>> inputSets = new ArrayList<>();
    inputSetReferences.forEach(identifier
        -> inputSets.add(pmsInputSetService.get(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false)));
    return MergeHelper.getInvalidInputSetReferences(inputSets, inputSetReferences);
  }

  public String getPipelineTemplate(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    Optional<PipelineEntity> optionalPipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (optionalPipelineEntity.isPresent()) {
      String pipelineYaml = optionalPipelineEntity.get().getYaml();
      try {
        return createTemplateFromPipeline(pipelineYaml);
      } catch (IOException e) {
        throw new InvalidRequestException("Could not convert pipeline to template");
      }
    } else {
      throw new InvalidRequestException("Could not find pipeline");
    }
  }

  public String getPipelineTemplate(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String pipelineBranch, String pipelineRepoID) {
    String pipelineYaml = getPipelineYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID);
    try {
      return createTemplateFromPipeline(pipelineYaml);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not convert pipeline to template");
    }
  }

  public String getMergeInputSetFromPipelineTemplate(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, List<String> inputSetReferences, String pipelineBranch, String pipelineRepoID) {
    String pipelineTemplate = getPipelineTemplate(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID);
    List<String> inputSetYamlList = new ArrayList<>();
    inputSetReferences.forEach(identifier -> {
      Optional<InputSetEntity> entity =
          pmsInputSetService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false);
      if (!entity.isPresent()) {
        throw new InvalidRequestException(identifier + " does not exist");
      }
      InputSetEntity inputSet = entity.get();
      if (inputSet.getInputSetEntityType() == InputSetEntityType.INPUT_SET) {
        inputSetYamlList.add(entity.get().getYaml());
      } else {
        List<String> overlayReferences = inputSet.getInputSetReferences();
        overlayReferences.forEach(id -> {
          Optional<InputSetEntity> entity2 =
              pmsInputSetService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, id, false);
          if (!entity2.isPresent()) {
            throw new InvalidRequestException(id + " does not exist");
          }
          inputSetYamlList.add(entity2.get().getYaml());
        });
      }
    });
    try {
      return mergeInputSets(pipelineTemplate, inputSetYamlList, false);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not merge input sets : " + e.getMessage());
    }
  }

  public String mergeInputSetIntoPipeline(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String mergedRuntimeInputYaml, String pipelineBranch, String pipelineRepoID) {
    String pipelineYaml = getPipelineYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, pipelineBranch, pipelineRepoID);
    try {
      return io.harness.pms.merger.helpers.MergeHelper.mergeInputSetIntoPipeline(
          pipelineYaml, mergedRuntimeInputYaml, false);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not merge input sets : " + e.getMessage());
    }
  }

  private void confirmPipelineIdentifier(String inputSetYaml, String pipelineIdentifier) {
    if (PMSInputSetElementMapper.isPipelineAbsent(inputSetYaml)) {
      throw new InvalidRequestException(
          "Input Set provides no values for any runtime input, or the pipeline has no runtime input");
    }
    String pipelineComponent = getPipelineComponent(inputSetYaml);
    String identifierInYaml = PMSInputSetElementMapper.getStringField(pipelineComponent, "identifier", "pipeline");
    if (!pipelineIdentifier.equals(identifierInYaml)) {
      throw new InvalidRequestException("Pipeline identifier in input set does not match");
    }
  }
}
