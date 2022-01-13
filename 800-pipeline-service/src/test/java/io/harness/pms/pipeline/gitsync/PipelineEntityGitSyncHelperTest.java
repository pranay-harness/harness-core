/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.gitsync;

import static io.harness.EntityType.PIPELINES;
import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.NAMAN;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.common.EntityReference;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.exception.UnexpectedException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.pipeline.service.PMSYamlSchemaService;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.protobuf.StringValue;
import java.io.IOException;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineEntityGitSyncHelperTest extends CategoryTest {
  @Mock private PMSPipelineService pipelineService;
  @Mock private PMSPipelineTemplateHelper pipelineTemplateHelper;
  @Mock private PMSYamlSchemaService pmsYamlSchemaService;
  @Mock private PmsFeatureFlagService pmsFeatureFlagService;
  @InjectMocks PipelineEntityGitSyncHelper pipelineEntityGitSyncHelper;
  static String accountId = "accountId";
  static String orgId = "orgId";
  static String projectId = "projectId";
  static String pipelineId = "pipelineId";
  static String name = "name";
  static String identifier = "identifier";
  static String pipelineYaml = "pipeline:\n"
      + "  identifier: p1\n"
      + "  name: pipeline1\n"
      + "  projectIdentifier: projectId\n"
      + "  orgIdentifier: orgId\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        identifier: managerDeployment\n"
      + "        type: deployment\n"
      + "        name: managerDeployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: manager\n"
      + "            name: manager\n"
      + "            serviceDefinition:\n"
      + "              type: k8s\n"
      + "              spec:\n"
      + "                field11: value1\n"
      + "                field12: value2\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              identifier: stagingInfra\n"
      + "              type: preProduction\n"
      + "              name: staging\n"
      + "            infrastructureDefinition:\n"
      + "              type: k8sDirect\n"
      + "              spec:\n"
      + "                connectorRef: pEIkEiNPSgSUsbWDDyjNKw\n"
      + "                namespace: harness\n"
      + "                releaseName: testingqa\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - step:\n"
      + "                  identifier: managerCanary\n"
      + "                  type: k8sCanary\n"
      + "                  spec:\n"
      + "                    field11: value1\n"
      + "                    field12: value2\n"
      + "              - step:\n"
      + "                  identifier: managerVerify\n"
      + "                  type: appdVerify\n"
      + "                  spec:\n"
      + "                    field21: value1\n"
      + "                    field22: value2\n"
      + "              - step:\n"
      + "                  identifier: managerRolling\n"
      + "                  type: k8sRolling\n"
      + "                  spec:\n"
      + "                    field31: value1\n"
      + "                    field32: value2";
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    doReturn(false).when(pmsFeatureFlagService).isEnabled(accountId, FeatureName.OPA_PIPELINE_GOVERNANCE);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGetEntityDetail() {
    PipelineEntity pipelineEntity = PipelineEntity.builder()
                                        .identifier(identifier)
                                        .name(name)
                                        .accountId(accountId)
                                        .orgIdentifier(orgId)
                                        .projectIdentifier(projectId)
                                        .build();

    EntityDetail entityDetail = pipelineEntityGitSyncHelper.getEntityDetail(pipelineEntity);
    assertEquals(entityDetail.getName(), name);
    assertEquals(entityDetail.getType(), PIPELINES);
    assertEquals(entityDetail.getEntityRef().getIdentifier(), identifier);
    assertEquals(entityDetail.getEntityRef().getOrgIdentifier(), orgId);
    assertEquals(entityDetail.getEntityRef().getAccountIdentifier(), accountId);
    assertEquals(entityDetail.getEntityRef().getProjectIdentifier(), projectId);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGetLastObjectIdIfExists() {
    String objectId = "objectId";
    doReturn(Optional.of(PipelineEntity.builder().objectIdOfYaml(objectId).build()))
        .when(pipelineService)
        .get(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    EntityGitDetails returnedEntityDetails =
        pipelineEntityGitSyncHelper.getEntityDetailsIfExists(accountId, pipelineYaml).get();
    verify(pipelineService, times(1)).get(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    assertEquals(returnedEntityDetails.getObjectId(), objectId);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testSave() throws IOException {
    doReturn(PipelineEntity.builder().orgIdentifier(orgId).projectIdentifier(projectId).yaml(pipelineYaml).build())
        .when(pipelineService)
        .create(any());
    TemplateMergeResponseDTO templateMergeResponseDTO =
        TemplateMergeResponseDTO.builder().mergedPipelineYaml(pipelineYaml).build();
    doReturn(templateMergeResponseDTO)
        .when(pipelineTemplateHelper)
        .resolveTemplateRefsInPipeline(PMSPipelineDtoMapper.toPipelineEntity(accountId, pipelineYaml));
    doNothing().when(pmsYamlSchemaService).validateYamlSchema(accountId, orgId, projectId, pipelineYaml);
    doNothing().when(pmsYamlSchemaService).validateUniqueFqn(pipelineYaml);
    PipelineConfig pipelineConfig = pipelineEntityGitSyncHelper.save(accountId, pipelineYaml);
    verify(pipelineService, times(1)).create(any());
    assertEquals(pipelineConfig, YamlUtils.read(pipelineYaml, PipelineConfig.class));
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testUpdate() throws IOException {
    PipelineEntity pipelineEntity =
        PipelineEntity.builder().orgIdentifier(orgId).projectIdentifier(projectId).yaml(pipelineYaml).build();
    doReturn(pipelineEntity).when(pipelineService).updatePipelineYaml(any(), any());
    TemplateMergeResponseDTO templateMergeResponseDTO =
        TemplateMergeResponseDTO.builder().mergedPipelineYaml(pipelineYaml).build();
    doReturn(templateMergeResponseDTO)
        .when(pipelineTemplateHelper)
        .resolveTemplateRefsInPipeline(PMSPipelineDtoMapper.toPipelineEntity(accountId, pipelineYaml));
    doNothing().when(pmsYamlSchemaService).validateYamlSchema(accountId, orgId, projectId, pipelineYaml);
    doNothing().when(pmsYamlSchemaService).validateUniqueFqn(pipelineYaml);
    PipelineConfig pipelineConfig = pipelineEntityGitSyncHelper.update(accountId, pipelineYaml, ChangeType.NONE);
    verify(pipelineService, times(1)).updatePipelineYaml(any(), any());
    assertEquals(pipelineConfig, YamlUtils.read(pipelineYaml, PipelineConfig.class));
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testDelete() {
    EntityReference entityReference = IdentifierRef.builder()
                                          .identifier(identifier)
                                          .accountIdentifier(accountId)
                                          .orgIdentifier(orgId)
                                          .projectIdentifier(projectId)
                                          .build();
    doReturn(true).when(pipelineService).delete(accountId, orgId, projectId, identifier, null);
    assertTrue(pipelineEntityGitSyncHelper.delete(entityReference));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testDeleteWithException() {
    EntityReference entityReference = IdentifierRef.builder()
                                          .identifier(identifier)
                                          .accountIdentifier(accountId)
                                          .orgIdentifier(orgId)
                                          .projectIdentifier(projectId)
                                          .build();
    when(pipelineService.delete(accountId, orgId, projectId, identifier, null))
        .thenThrow(new EventsFrameworkDownException("something wrong"));
    assertThatThrownBy(() -> pipelineEntityGitSyncHelper.delete(entityReference))
        .isInstanceOf(UnexpectedException.class)
        .hasMessage("Producer shutdown: EventsFrameworkDownException: something wrong");
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testKeyGetters() {
    assertEquals(pipelineEntityGitSyncHelper.getEntityType(), PIPELINES);
    assertEquals(pipelineEntityGitSyncHelper.getObjectIdOfYamlKey(), PipelineEntityKeys.objectIdOfYaml);
    assertEquals(pipelineEntityGitSyncHelper.getIsFromDefaultBranchKey(), PipelineEntityKeys.isFromDefaultBranch);
    assertEquals(pipelineEntityGitSyncHelper.getYamlGitConfigRefKey(), PipelineEntityKeys.yamlGitConfigRef);
    assertEquals(pipelineEntityGitSyncHelper.getUuidKey(), PipelineEntityKeys.uuid);
    assertEquals(pipelineEntityGitSyncHelper.getBranchKey(), PipelineEntityKeys.branch);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetYamlFromEntityRef() {
    EntityDetailProtoDTO entityDetailProtoDTO =
        EntityDetailProtoDTO.newBuilder()
            .setIdentifierRef(IdentifierRefProtoDTO.newBuilder()
                                  .setAccountIdentifier(StringValue.of(accountId))
                                  .setOrgIdentifier(StringValue.of(orgId))
                                  .setProjectIdentifier(StringValue.of(projectId))
                                  .setIdentifier(StringValue.of(pipelineId))
                                  .build())
            .build();
    doReturn(Optional.of(PipelineEntity.builder().yaml(pipelineYaml).build()))
        .when(pipelineService)
        .get(accountId, orgId, projectId, pipelineId, false);
    String yamlFromEntityRef = pipelineEntityGitSyncHelper.getYamlFromEntityRef(entityDetailProtoDTO);
    assertEquals(yamlFromEntityRef, pipelineYaml);
  }
}
