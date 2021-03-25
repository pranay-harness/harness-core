package io.harness.pms.ngpipeline.inputset.helpers;

import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.pms.merger.helpers.MergeHelper.createTemplateFromPipeline;
import static io.harness.pms.merger.helpers.MergeHelper.getPipelineComponent;
import static io.harness.pms.merger.helpers.MergeHelper.mergeInputSets;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.inputset.helpers.MergeHelper;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetErrorWrapperDTOPMS;
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

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class ValidateAndMergeHelper {
  private final PMSPipelineService pmsPipelineService;
  private final PMSInputSetService pmsInputSetService;

  public InputSetErrorWrapperDTOPMS validateInputSet(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    String identifier = PMSInputSetElementMapper.getStringField(yaml, "identifier", "inputSet");
    if (hasNone(identifier)) {
      throw new InvalidRequestException("Identifier cannot be empty");
    }
    confirmPipelineIdentifier(yaml, pipelineIdentifier);

    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (pipelineEntity.isPresent()) {
      String pipelineYaml = pipelineEntity.get().getYaml();
      try {
        return MergeHelper.getErrorMap(pipelineYaml, yaml);

      } catch (IOException e) {
        throw new InvalidRequestException("Invalid input set yaml");
      }
    } else {
      throw new InvalidRequestException("Pipeline does not exist");
    }
  }

  public Map<String, String> validateOverlayInputSet(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, InputSetEntity entity) {
    if (hasNone(entity.getIdentifier())) {
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

  public String getMergeInputSetFromPipelineTemplate(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, List<String> inputSetReferences) {
    String pipelineTemplate = getPipelineTemplate(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
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

  private void confirmPipelineIdentifier(String inputSetYaml, String pipelineIdentifier) {
    if (PMSInputSetElementMapper.isPipelineAbsent(inputSetYaml)) {
      throw new InvalidRequestException("Input Set provides no values for any runtime input");
    }
    String pipelineComponent = getPipelineComponent(inputSetYaml);
    String identifierInYaml = PMSInputSetElementMapper.getStringField(pipelineComponent, "identifier", "pipeline");
    if (!pipelineIdentifier.equals(identifierInYaml)) {
      throw new InvalidRequestException("Pipeline identifier in input set does not match");
    }
  }
}
