package io.harness.cdng.inputset.helpers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.services.InputSetEntityService;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.BaseInputSetEntity;
import io.harness.ngpipeline.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.inputset.InputSetTemplateVisitor;
import io.harness.walktree.visitor.mergeinputset.MergeInputSetVisitor;
import io.harness.walktree.visitor.mergeinputset.beans.MergeVisitorInputSetElement;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InputSetMergeHelper {
  private final InputSetEntityService inputSetEntityService;
  private final PipelineService ngPipelineService;
  private final SimpleVisitorFactory simpleVisitorFactory;

  /**
   * This function return input set template pipeline yaml.
   */
  public String getTemplateFromPipeline(String pipelineYaml) {
    try {
      CDPipeline result = getTemplateObjectFromPipelineYaml(pipelineYaml);
      return JsonPipelineUtils.writeYamlString(result).replaceAll("---\n", "");
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to template");
    }
  }

  private CDPipeline getTemplateObjectFromPipelineYaml(String pipelineYaml) {
    InputSetTemplateVisitor visitor = simpleVisitorFactory.obtainInputSetTemplateVisitor();
    try {
      CDPipeline pipeline = YamlPipelineUtils.read(pipelineYaml, CDPipeline.class);
      visitor.walkElementTree(pipeline);
      return (CDPipeline) visitor.getCurrentObject();
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to template");
    }
  }

  /**
   * This function gives Pipeline object as response based on isTemplateResponse, which if true will set only template
   * values marked as runtime in original pipeline.
   * inputSetIdentifierList is ordered, the entry added to the end of list will be given highest priority.
   */
  public MergeInputSetResponse getMergePipelineYamlFromInputIdentifierList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetIdentifierList,
      boolean isTemplateResponse, boolean useFQNIfErrorResponse) {
    Optional<CDPipelineResponseDTO> optionalPipeline =
        ngPipelineService.getPipeline(pipelineIdentifier, accountId, orgIdentifier, projectIdentifier);
    if (optionalPipeline.isPresent()) {
      CDPipelineResponseDTO cdPipelineResponseDTO = optionalPipeline.get();
      CDPipeline originalPipeline = cdPipelineResponseDTO.getCdPipeline();
      String pipelineYaml = cdPipelineResponseDTO.getYamlPipeline();

      if (isTemplateResponse) {
        originalPipeline = getTemplateObjectFromPipelineYaml(pipelineYaml);
      }
      List<MergeVisitorInputSetElement> inputSetPipelineElementList = getInputSetOrderedListPipelineElement(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifierList);
      MergeInputSetVisitor mergeInputSetVisitor =
          simpleVisitorFactory.obtainMergeInputSetVisitor(useFQNIfErrorResponse, inputSetPipelineElementList);
      mergeInputSetVisitor.walkElementTree(originalPipeline);
      return toMergeInputSetResponse(mergeInputSetVisitor);
    } else {
      throw new InvalidRequestException("Pipeline doesn't exist for given identifier: " + pipelineIdentifier);
    }
  }

  /**
   * This function gets ordered inputSet element list by getting inputset /overlay input set from db.
   */
  private List<MergeVisitorInputSetElement> getInputSetOrderedListPipelineElement(String accountId,
      String orgIdentifier, String projectIdentifier, String pipelineIdentifier,
      List<String> originalInputSetIdentifierList) {
    List<BaseInputSetEntity> baseInputSetEntities = inputSetEntityService.getGivenInputSetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, new HashSet<>(originalInputSetIdentifierList));

    List<BaseInputSetEntity> inputSetListInOverlaySet = getInputSetListInOverlaySet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, baseInputSetEntities);

    Map<String, BaseInputSetEntity> identifierToInputSetEntityMap = baseInputSetEntities.stream().collect(
        Collectors.toMap(BaseInputSetEntity::getIdentifier, baseInputSetEntity -> baseInputSetEntity, (a, b) -> b));
    inputSetListInOverlaySet.forEach(baseInputSetEntity
        -> identifierToInputSetEntityMap.put(baseInputSetEntity.getIdentifier(), baseInputSetEntity));

    List<MergeVisitorInputSetElement> inputSetPipelineElementList = new LinkedList<>();

    for (String inputSetIdentifier : originalInputSetIdentifierList) {
      if (identifierToInputSetEntityMap.containsKey(inputSetIdentifier)) {
        BaseInputSetEntity baseInputSetEntity = identifierToInputSetEntityMap.get(inputSetIdentifier);

        // Input Set
        if (baseInputSetEntity.getInputSetType() == InputSetEntityType.INPUT_SET) {
          MergeVisitorInputSetElement element = getVisitorInputSetElement(inputSetIdentifier, baseInputSetEntity);
          inputSetPipelineElementList.add(element);
        }
        // OverlayInputSet
        else if (baseInputSetEntity.getInputSetType() == InputSetEntityType.OVERLAY_INPUT_SET) {
          OverlayInputSetEntity overlayInputSetEntity = (OverlayInputSetEntity) baseInputSetEntity;
          for (String inputSetReference : overlayInputSetEntity.getInputSetReferences()) {
            if (identifierToInputSetEntityMap.containsKey(inputSetReference)) {
              BaseInputSetEntity baseInputSetEntityReference = identifierToInputSetEntityMap.get(inputSetReference);
              // Input Set
              if (baseInputSetEntityReference.getInputSetType() == InputSetEntityType.INPUT_SET) {
                MergeVisitorInputSetElement element =
                    getVisitorInputSetElement(inputSetReference, baseInputSetEntityReference);
                inputSetPipelineElementList.add(element);
              }
              // Overlay InputSet cannot contain another overlay InputSetIdentifier.
              else {
                throw new InvalidArgumentsException(
                    "Input set identifier doesn't exist, please send valid input set identifier.");
              }
            } else {
              throw new InvalidArgumentsException(
                  "Input set identifier doesn't exist, please send valid input set identifier.");
            }
          }
        }

      } else {
        throw new InvalidArgumentsException(
            "Input set identifier doesn't exist, please send valid input set identifier.");
      }
    }

    return inputSetPipelineElementList;
  }

  /**
   * Helper function to get VisitorInputSetElement.
   */
  private MergeVisitorInputSetElement getVisitorInputSetElement(
      String inputSetIdentifier, BaseInputSetEntity baseInputSetEntity) {
    if (baseInputSetEntity.getInputSetType() == InputSetEntityType.INPUT_SET) {
      CDInputSetEntity inputSetEntity = (CDInputSetEntity) baseInputSetEntity;
      return MergeVisitorInputSetElement.builder()
          .identifier(inputSetIdentifier)
          .inputSetElement(inputSetEntity.getCdInputSet().getPipeline())
          .build();
    }
    return null;
  }

  /**
   * This function returns input set defined as references inside overlay input set.
   */
  private List<BaseInputSetEntity> getInputSetListInOverlaySet(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<BaseInputSetEntity> baseInputSetEntities) {
    Set<OverlayInputSetEntity> overlayInputSetIdentifiers = getOverlayInputSetIdentifiers(baseInputSetEntities);
    Set<String> notFetchedInputSetIdentifiers =
        filterInputSetIdentifierNotFetched(overlayInputSetIdentifiers, baseInputSetEntities);
    return inputSetEntityService.getGivenInputSetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, notFetchedInputSetIdentifiers);
  }

  /**
   * Helper function get overlay input set identifier from BaseInputSetEntity.
   */
  private Set<OverlayInputSetEntity> getOverlayInputSetIdentifiers(List<BaseInputSetEntity> baseInputSetEntities) {
    return baseInputSetEntities.stream()
        .filter(baseInputSetEntity -> baseInputSetEntity.getInputSetType() == InputSetEntityType.OVERLAY_INPUT_SET)
        .map(baseInputSetEntity -> (OverlayInputSetEntity) baseInputSetEntity)
        .collect(Collectors.toSet());
  }

  /**
   * Helper function to filter input set identifiers which are not yet fetched fom db.
   */
  private Set<String> filterInputSetIdentifierNotFetched(
      Set<OverlayInputSetEntity> overlayInputSetEntities, List<BaseInputSetEntity> baseInputSetEntities) {
    Set<String> fetchedInputSetIdentifierSet =
        baseInputSetEntities.stream()
            .filter(baseInputSetEntity -> baseInputSetEntity.getInputSetType() == InputSetEntityType.INPUT_SET)
            .map(BaseInputSetEntity::getIdentifier)
            .collect(Collectors.toSet());

    // return set of input set identifier (inside overlay input set) which are not yet fetched.
    return overlayInputSetEntities.stream()
        .flatMap(overlayInputSetEntity -> overlayInputSetEntity.getInputSetReferences().stream())
        .filter(inputSetIdentifier -> !fetchedInputSetIdentifierSet.contains(inputSetIdentifier))
        .collect(Collectors.toSet());
  }

  /**
   * Function to create MergeInputSet response to combine the result.
   */
  private MergeInputSetResponse toMergeInputSetResponse(MergeInputSetVisitor mergeInputSetVisitor) {
    try {
      boolean isErrorResponse = !mergeInputSetVisitor.isResultValidInputSet();
      String pipelineResponse = "";
      String errorPipelineResponse = "";
      if (isErrorResponse) {
        errorPipelineResponse = JsonPipelineUtils.writeYamlString(mergeInputSetVisitor.getCurrentObjectErrorResult())
                                    .replaceAll("---\n", "");
      } else {
        pipelineResponse =
            JsonPipelineUtils.writeYamlString(mergeInputSetVisitor.getCurrentObjectResult()).replaceAll("---\n", "");
      }

      Map<String, VisitorErrorResponseWrapper> uuidToErrorResponseMap = new HashMap<>();
      mergeInputSetVisitor.getUuidToErrorResponseMap().forEach((key, value) -> {
        if (value instanceof VisitorErrorResponseWrapper) {
          uuidToErrorResponseMap.put(key, (VisitorErrorResponseWrapper) value);
        } else {
          throw new InvalidArgumentsException(
              "Error Response from merge input set visitor should be of VisitorErrorResponseWrapper instance.");
        }
      });

      return MergeInputSetResponse.builder()
          .pipelineYaml(pipelineResponse)
          .isErrorResponse(isErrorResponse)
          .errorPipelineYaml(errorPipelineResponse)
          .uuidToErrorResponseMap(uuidToErrorResponseMap)
          .build();
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to yaml.");
    }
  }
}
