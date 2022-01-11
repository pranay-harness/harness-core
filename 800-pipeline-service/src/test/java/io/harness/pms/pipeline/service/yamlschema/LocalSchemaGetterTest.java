/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service.yamlschema;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.service.yamlschema.approval.ApprovalYamlSchemaService;
import io.harness.pms.pipeline.service.yamlschema.featureflag.FeatureFlagYamlService;
import io.harness.rule.Owner;
import io.harness.yaml.schema.YamlSchemaProvider;
import io.harness.yaml.schema.beans.PartialSchemaDTO;
import io.harness.yaml.schema.beans.YamlGroup;
import io.harness.yaml.schema.beans.YamlSchemaDetailsWrapper;
import io.harness.yaml.schema.beans.YamlSchemaMetadata;
import io.harness.yaml.schema.beans.YamlSchemaWithDetails;

import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class LocalSchemaGetterTest {
  @Mock private YamlSchemaProvider yamlSchemaProvider;
  @Mock private ApprovalYamlSchemaService approvalYamlSchemaService;
  @Mock private FeatureFlagYamlService featureFlagYamlService;
  @Mock private PmsYamlSchemaHelper pmsYamlSchemaHelper;
  @InjectMocks LocalSchemaGetter localSchemaGetter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGetSchema() {
    PartialSchemaDTO approvalPartialSchemaDTO =
        PartialSchemaDTO.builder().namespace("approval").moduleType(ModuleType.PMS).nodeName("approval").build();
    PartialSchemaDTO cfPartialSchemaDTO =
        PartialSchemaDTO.builder().namespace("cf").moduleType(ModuleType.PMS).nodeName("cf").build();

    doReturn(approvalPartialSchemaDTO).when(approvalYamlSchemaService).getApprovalYamlSchema(any(), any(), any());
    doReturn(cfPartialSchemaDTO).when(featureFlagYamlService).getFeatureFlagYamlSchema(any(), any(), any());

    List<PartialSchemaDTO> partialSchemaDTOList = localSchemaGetter.getSchema(Collections.emptyList());
    assertEquals(partialSchemaDTOList.size(), 2);
    assertEquals(partialSchemaDTOList.get(1), cfPartialSchemaDTO);
    assertEquals(partialSchemaDTOList.get(0), approvalPartialSchemaDTO);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGetSchemaDetails() {
    YamlSchemaMetadata yamlSchemaMetadata = YamlSchemaMetadata.builder()
                                                .yamlGroup(YamlGroup.builder().group("step").build())
                                                .modulesSupported(Collections.singletonList(ModuleType.CD))
                                                .build();
    YamlSchemaWithDetails yamlSchemaWithDetails = YamlSchemaWithDetails.builder()
                                                      .yamlSchemaMetadata(yamlSchemaMetadata)
                                                      .moduleType(ModuleType.PMS)
                                                      .schemaClassName("HttpStepNode")
                                                      .build();
    doReturn(Collections.singletonList(yamlSchemaWithDetails))
        .when(yamlSchemaProvider)
        .getCrossFunctionalStepsSchemaDetails(any(), any(), any(), any(), any());
    YamlSchemaDetailsWrapper yamlSchemaDetailsWrapper = localSchemaGetter.getSchemaDetails();

    assertEquals(yamlSchemaDetailsWrapper.getYamlSchemaWithDetailsList().size(), 1);
    assertEquals(yamlSchemaDetailsWrapper.getYamlSchemaWithDetailsList().get(0).getModuleType(), ModuleType.PMS);
    assertEquals(yamlSchemaDetailsWrapper.getYamlSchemaWithDetailsList().get(0).getSchemaClassName(), "HttpStepNode");
    assertEquals(
        yamlSchemaDetailsWrapper.getYamlSchemaWithDetailsList().get(0).getYamlSchemaMetadata(), yamlSchemaMetadata);
  }
}
