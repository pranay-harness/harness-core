package io.harness.cdng.inputset.helpers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.inputset.beans.entities.InputSetEntity;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.services.InputSetEntityService;
import io.harness.cdng.pipeline.NgPipeline;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InputSetEntityValidationHelper {
  private final InputSetMergeHelper inputSetMergeHelper;
  private final InputSetEntityService inputSetEntityService;

  private final String identifierIsOverlayMsg = "References can't be other overlay input sets";
  private final String identifierNotFoundMsg = "Reference does not exist";

  public MergeInputSetResponse validateInputSetEntity(InputSetEntity inputSetEntity) {
    NgPipeline pipeline = inputSetEntity.getInputSetConfig().getPipeline();
    return inputSetMergeHelper.getMergePipelineYamlFromInputSetPipelineYaml(inputSetEntity.getAccountId(),
        inputSetEntity.getOrgIdentifier(), inputSetEntity.getProjectIdentifier(),
        inputSetEntity.getPipelineIdentifier(), inputSetEntity.getIdentifier(), pipeline, true, false);
  }

  public Map<String, String> validateOverlayInputSetEntity(OverlayInputSetEntity overlayInputSetEntity) {
    List<String> allReferencesList = overlayInputSetEntity.getInputSetReferences();
    Set<String> allReferencesInOverlaySet = new HashSet<>(allReferencesList);
    List<BaseInputSetEntity> referencesFoundInDB =
        inputSetEntityService.getGivenInputSetList(overlayInputSetEntity.getAccountId(),
            overlayInputSetEntity.getOrgIdentifier(), overlayInputSetEntity.getProjectIdentifier(),
            overlayInputSetEntity.getPipelineIdentifier(), allReferencesInOverlaySet);

    Map<String, String> invalidIdentifiers = new HashMap<>();

    Set<String> identifiersFoundInDB = new HashSet<>();
    for (BaseInputSetEntity entity : referencesFoundInDB) {
      if (entity.getInputSetType() == InputSetEntityType.OVERLAY_INPUT_SET) {
        invalidIdentifiers.put(entity.getIdentifier(), identifierIsOverlayMsg);
      }
      identifiersFoundInDB.add(entity.getIdentifier());
    }

    for (String identifier : allReferencesInOverlaySet) {
      if (!identifiersFoundInDB.contains(identifier)) {
        invalidIdentifiers.put(identifier, identifierNotFoundMsg);
      }
    }
    return invalidIdentifiers;
  }
}
