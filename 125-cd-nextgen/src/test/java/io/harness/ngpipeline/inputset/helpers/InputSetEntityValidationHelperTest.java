package io.harness.ngpipeline.inputset.helpers;

import static io.harness.rule.OwnerRule.NAMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.services.impl.InputSetEntityServiceImpl;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputSetEntityValidationHelperTest extends CategoryTest {
  @Mock InputSetEntityServiceImpl inputSetEntityService;
  @Mock InputSetMergeHelper inputSetMergeHelper;
  @InjectMocks InputSetEntityValidationHelper validationHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testValidateOverlayInputSetEntity() {
    String identifierIsOverlayMsg = "References can't be other overlay input sets";
    String identifierNotFoundMsg = "Reference does not exist";

    List<String> overlayReferences = new ArrayList<>();
    overlayReferences.add("inputSet0");
    overlayReferences.add("inputSet1");
    overlayReferences.add("inputSet2");
    overlayReferences.add("inputSet3");
    OverlayInputSetEntity entity = OverlayInputSetEntity.builder().inputSetReferences(overlayReferences).build();

    List<BaseInputSetEntity> referencesInDB = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      BaseInputSetEntity inputSetEntity = InputSetEntity.builder().build();
      inputSetEntity.setIdentifier("inputSet" + i);
      inputSetEntity.setInputSetType(InputSetEntityType.INPUT_SET);
      referencesInDB.add(inputSetEntity);
    }
    doReturn(referencesInDB).when(inputSetEntityService).getGivenInputSetList(any(), any(), any(), any(), any());

    Map<String, String> res = validationHelper.validateOverlayInputSetEntity(entity);
    assertThat(res).isEmpty();

    // adding a non existent input set
    overlayReferences.add("inputSet4");
    entity = OverlayInputSetEntity.builder().inputSetReferences(overlayReferences).build();
    res = validationHelper.validateOverlayInputSetEntity(entity);
    assertThat(res.size()).isEqualTo(1);
    assertThat(res).containsKey("inputSet4");
    assertThat(res.get("inputSet4")).isEqualTo(identifierNotFoundMsg);

    // adding an overlay input set
    overlayReferences.add("overlayInputSet");
    entity = OverlayInputSetEntity.builder().inputSetReferences(overlayReferences).build();

    BaseInputSetEntity overlaySet = OverlayInputSetEntity.builder().build();
    overlaySet.setInputSetType(InputSetEntityType.OVERLAY_INPUT_SET);
    overlaySet.setIdentifier("overlayInputSet");
    referencesInDB.add(overlaySet);
    doReturn(referencesInDB).when(inputSetEntityService).getGivenInputSetList(any(), any(), any(), any(), any());

    res = validationHelper.validateOverlayInputSetEntity(entity);
    assertThat(res.size()).isEqualTo(2);
    assertThat(res).containsKey("inputSet4");
    assertThat(res.get("inputSet4")).isEqualTo(identifierNotFoundMsg);
    assertThat(res).containsKey("overlayInputSet");
    assertThat(res.get("overlayInputSet")).isEqualTo(identifierIsOverlayMsg);
  }
}
