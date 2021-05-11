package io.harness.ngpipeline.inputset.helpers;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.inputset.services.InputSetEntityService;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.walktree.visitor.SimpleVisitorFactory;
import io.harness.walktree.visitor.inputset.InputSetTemplateVisitor;
import io.harness.walktree.visitor.mergeinputset.MergeInputSetVisitor;
import io.harness.walktree.visitor.mergeinputset.beans.MergeVisitorInputSetElement;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@ToBeDeleted
@Deprecated
public class InputSetMergeHelper {
  private final InputSetEntityService inputSetEntityService;
  private final NGPipelineService ngPipelineService;
  private final SimpleVisitorFactory simpleVisitorFactory;

  /**
   * This function return input set template pipeline yaml.
   */
  public String getTemplateFromPipeline(String pipelineYaml) {
    try {
      NgPipeline result = getTemplateObjectFromPipelineYaml(pipelineYaml, true);
      if (result == null) {
        return "";
      }
      return JsonPipelineUtils.writeYamlString(result).replaceAll("---\n", "");
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to template");
    }
  }

  private NgPipeline getTemplateObjectFromPipelineYaml(String pipelineYaml, boolean keepRuntimeInput) {
    try {
      return getTemplateObjectFromPipelineObject(
          YamlPipelineUtils.read(pipelineYaml, NgPipeline.class), keepRuntimeInput);
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to template");
    }
  }

  public NgPipeline getTemplateObjectFromPipelineObject(NgPipeline pipeline, boolean keepRuntimeInput) {
    InputSetTemplateVisitor visitor = simpleVisitorFactory.obtainInputSetTemplateVisitor(keepRuntimeInput);
    visitor.walkElementTree(pipeline);
    return (NgPipeline) visitor.getCurrentObject();
  }

  /**
   * This is merge input Set helper function, which takes single inputset yaml which is yaml format.
   * Note :- inputSetPipelineYaml should be of format  {@link NgPipeline }
   */
  public MergeInputSetResponse getMergePipelineYamlFromInputSetPipelineYaml(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String inputSetPipelineYaml, boolean isTemplateResponse,
      boolean useFQNIfErrorResponse) {
    NgPipeline inputSetPipeline = getTemplateObjectFromPipelineYaml(inputSetPipelineYaml, false);
    return getMergePipelineYamlFromInputSetPipelineYaml(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier,
        "inputSet", inputSetPipeline, isTemplateResponse, useFQNIfErrorResponse);
  }

  /**
   * This function gives Pipeline object as response based on isTemplateResponse, which if true will set only
   * template values marked as runtime in original pipeline. There is only one inputSetPipeline as input.
   */
  public MergeInputSetResponse getMergePipelineYamlFromInputSetPipelineYaml(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String inputSetIdentifier, NgPipeline inputSetPipeline,
      boolean isTemplateResponse, boolean useFQNIfErrorResponse) {
    NgPipeline originalPipeline = getOriginalOrTemplatePipeline(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, isTemplateResponse);
    List<MergeVisitorInputSetElement> inputSetPipelineElementList =
        Collections.singletonList(MergeVisitorInputSetElement.builder()
                                      .inputSetIdentifier(inputSetIdentifier)
                                      .inputSetElement(inputSetPipeline)
                                      .build());
    return runMergeInputSetVisitor(originalPipeline, inputSetPipelineElementList, useFQNIfErrorResponse);
  }

  /**
   * This function gives Pipeline object as response based on isTemplateResponse, which if true will set only
   * template values marked as runtime in original pipeline. inputSetIdentifierList is ordered, the entry added to
   * the end of list will be given highest priority.
   */
  public MergeInputSetResponse getMergePipelineYamlFromInputIdentifierList(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, List<String> inputSetIdentifierList,
      boolean isTemplateResponse, boolean useFQNIfErrorResponse) {
    NgPipeline originalPipeline = getOriginalOrTemplatePipeline(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, isTemplateResponse);
    List<MergeVisitorInputSetElement> inputSetPipelineElementList = getInputSetOrderedListPipelineElement(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifierList);
    return runMergeInputSetVisitor(originalPipeline, inputSetPipelineElementList, useFQNIfErrorResponse);
  }

  public NgPipeline getOriginalOrTemplatePipeline(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, boolean isTemplateResponse) {
    Optional<NgPipelineEntity> optionalNgPipelineEntity =
        ngPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!optionalNgPipelineEntity.isPresent()) {
      throw new InvalidRequestException("Pipeline doesn't exist for given identifier: " + pipelineIdentifier);
    }
    NgPipelineEntity ngPipelineEntity = optionalNgPipelineEntity.get();

    NgPipeline originalPipeline = ngPipelineEntity.getNgPipeline();
    String pipelineYaml = ngPipelineEntity.getYamlPipeline();

    if (isTemplateResponse) {
      originalPipeline = getTemplateObjectFromPipelineYaml(pipelineYaml, true);
    }
    return originalPipeline;
  }

  private MergeInputSetResponse runMergeInputSetVisitor(NgPipeline originalPipeline,
      List<MergeVisitorInputSetElement> inputSetPipelineElementList, boolean useFQNIfErrorResponse) {
    MergeInputSetVisitor mergeInputSetVisitor =
        simpleVisitorFactory.obtainMergeInputSetVisitor(useFQNIfErrorResponse, inputSetPipelineElementList);
    mergeInputSetVisitor.walkElementTree(originalPipeline);
    return toMergeInputSetResponse(mergeInputSetVisitor);
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
                    "Input set identifier doesn't exist, please send valid input set identifier - " + inputSetReference
                    + " which is defined in overlay identifier - " + inputSetIdentifier);
              }
            } else {
              throw new InvalidArgumentsException(
                  "Input set identifier doesn't exist, please send valid input set identifier - " + inputSetReference
                  + " which is defined in overlay identifier - " + inputSetIdentifier);
            }
          }
        }

      } else {
        throw new InvalidArgumentsException(
            "Input set identifier doesn't exist, please send valid input set identifier - " + inputSetIdentifier);
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
      InputSetEntity inputSetEntity = (InputSetEntity) baseInputSetEntity;
      return MergeVisitorInputSetElement.builder()
          .inputSetIdentifier(inputSetIdentifier)
          .inputSetElement(inputSetEntity.getInputSetConfig().getPipeline())
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
    boolean isErrorResponse = !mergeInputSetVisitor.isResultValidInputSet();
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
        .mergedPipeline((NgPipeline) mergeInputSetVisitor.getCurrentObjectResult())
        .isErrorResponse(isErrorResponse)
        .errorPipeline((NgPipeline) mergeInputSetVisitor.getCurrentObjectErrorResult())
        .uuidToErrorResponseMap(uuidToErrorResponseMap)
        .build();
  }

  public InputSetEntity removeRuntimeInputs(InputSetEntity entity) {
    InputSetTemplateVisitor visitor = simpleVisitorFactory.obtainInputSetTemplateVisitor(false);
    visitor.walkElementTree(entity.getInputSetConfig().getPipeline());
    if (!visitor.isEntityChanged()) {
      return entity;
    }
    NgPipeline clearedContents = (NgPipeline) visitor.getCurrentObject();
    InputSetConfig clearedConfig = InputSetConfig.builder()
                                       .identifier(entity.getIdentifier())
                                       .name(entity.getName())
                                       .description(entity.getDescription())
                                       .pipeline(clearedContents)
                                       .build();
    String clearedYaml;
    try {
      clearedYaml = JsonPipelineUtils.writeYamlString(clearedConfig).replaceAll("---\n", "");
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to template" + e.getMessage());
    }
    entity.setInputSetConfig(clearedConfig);
    entity.setInputSetYaml(clearedYaml);

    return entity;
  }
}
