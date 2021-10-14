package io.harness.pms.pipeline.gitsync;

import static io.harness.EntityType.PIPELINES;
import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.common.EntityReference;
import io.harness.git.model.ChangeType;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.mappers.PMSPipelineDtoMapper;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.pipeline.service.PMSPipelineTemplateHelper;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

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
    String returnedObjectId = pipelineEntityGitSyncHelper.getLastObjectIdIfExists(accountId, pipelineYaml);
    verify(pipelineService, times(1)).get(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    assertEquals(returnedObjectId, objectId);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testSave() throws IOException {
    doReturn(PipelineEntity.builder().yaml(pipelineYaml).build()).when(pipelineService).create(any());
    TemplateMergeResponseDTO templateMergeResponseDTO =
        TemplateMergeResponseDTO.builder().mergedPipelineYaml(pipelineYaml).build();
    doReturn(templateMergeResponseDTO)
        .when(pipelineTemplateHelper)
        .resolveTemplateRefsInPipeline(PMSPipelineDtoMapper.toPipelineEntity(accountId, pipelineYaml));
    PipelineConfig pipelineConfig = pipelineEntityGitSyncHelper.save(accountId, pipelineYaml);
    verify(pipelineService, times(1)).create(any());
    assertEquals(pipelineConfig, YamlUtils.read(pipelineYaml, PipelineConfig.class));
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testUpdate() throws IOException {
    PipelineEntity pipelineEntity = PipelineEntity.builder().yaml(pipelineYaml).build();
    doReturn(pipelineEntity).when(pipelineService).updatePipelineYaml(any(), any());
    TemplateMergeResponseDTO templateMergeResponseDTO =
        TemplateMergeResponseDTO.builder().mergedPipelineYaml(pipelineYaml).build();
    doReturn(templateMergeResponseDTO).when(pipelineTemplateHelper).resolveTemplateRefsInPipeline(pipelineEntity);
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
}
