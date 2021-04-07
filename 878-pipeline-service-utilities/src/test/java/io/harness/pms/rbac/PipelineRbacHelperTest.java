package io.harness.pms.rbac;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.EntityType;
import io.harness.accesscontrol.clients.AccessCheckResponseDTO;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.AccessControlDTO;
import io.harness.accesscontrol.clients.PermissionCheckDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.entitydetail.EntityDetailProtoToRestMapper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.plan.PrincipalType;
import io.harness.rule.Owner;

import io.fabric8.utils.Lists;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineRbacHelperTest extends CategoryTest {
  private static String ACCOUNT_ID = "accountId";
  private static String ORG_ID = "orgId";
  private static String PROJECT_ID = "projectId";

  @Mock EntityDetailProtoToRestMapper entityDetailProtoToRestMapper;
  @Mock AccessControlClient accessControlClient;

  @InjectMocks PipelineRbacHelper pipelineRbacHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyInteractions() {
    verifyNoMoreInteractions(accessControlClient);
    verifyNoMoreInteractions(entityDetailProtoToRestMapper);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testCheckRuntimePermissions() {
    List<EntityDetail> entityDetails = getEntityDetailsWithoutMetadata();
    when(entityDetailProtoToRestMapper.createEntityDetailsDTO(Mockito.anyList())).thenReturn(entityDetails);
    Ambiance ambiance = Ambiance.newBuilder()
                            .setMetadata(ExecutionMetadata.newBuilder()
                                             .setPrincipalInfo(ExecutionPrincipalInfo.newBuilder()
                                                                   .setPrincipal("princ")
                                                                   .setPrincipalType(PrincipalType.USER)
                                                                   .build())
                                             .build())
                            .build();
    AccessCheckResponseDTO accessCheckResponseDTO =
        AccessCheckResponseDTO.builder()
            .accessControlList(Collections.singletonList(AccessControlDTO.builder().permitted(true).build()))
            .build();
    when(accessControlClient.checkForAccess(
             Mockito.eq("princ"), Mockito.eq(io.harness.accesscontrol.principals.PrincipalType.USER), anyList()))
        .thenReturn(accessCheckResponseDTO);

    pipelineRbacHelper.checkRuntimePermissions(ambiance, new HashSet<>());

    verify(entityDetailProtoToRestMapper).createEntityDetailsDTO(Mockito.anyList());
    verify(accessControlClient)
        .checkForAccess(
            Mockito.eq("princ"), Mockito.eq(io.harness.accesscontrol.principals.PrincipalType.USER), anyList());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testConvertToPermissionCheckDTO() {
    EntityDetail entityDetailWithMetaData = getEntityDetailWithMetadata();
    EntityDetail entityDetail1WithoutMetaData = getEntityDetailWithoutMetadata();

    PermissionCheckDTO permissionCheckDTO = pipelineRbacHelper.convertToPermissionCheckDTO(entityDetailWithMetaData);

    assertThat(permissionCheckDTO.getPermission()).isEqualTo("core_connector_edit");

    PermissionCheckDTO permissionCheckDTO1 =
        pipelineRbacHelper.convertToPermissionCheckDTO(entityDetail1WithoutMetaData);

    assertThat(permissionCheckDTO1.getPermission()).isEqualTo("core_connector_runtimeAccess");
  }

  private List<EntityDetail> getEntityDetailsWithoutMetadata() {
    EntityDetail entityDetailProjectLevel = EntityDetail.builder()
                                                .entityRef(IdentifierRef.builder()
                                                               .accountIdentifier(ACCOUNT_ID)
                                                               .orgIdentifier(ORG_ID)
                                                               .projectIdentifier(PROJECT_ID)
                                                               .identifier("id1")
                                                               .build())
                                                .type(EntityType.CONNECTORS)
                                                .build();
    EntityDetail entityDetailOrgLevel =
        EntityDetail.builder()
            .entityRef(
                IdentifierRef.builder().accountIdentifier(ACCOUNT_ID).orgIdentifier(ORG_ID).identifier("id2").build())
            .type(EntityType.CONNECTORS)
            .build();
    EntityDetail entityDetailAccountLevel =
        EntityDetail.builder()
            .entityRef(IdentifierRef.builder().accountIdentifier(ACCOUNT_ID).identifier("id3").build())
            .type(EntityType.CONNECTORS)
            .build();

    return Lists.newArrayList(entityDetailAccountLevel, entityDetailOrgLevel, entityDetailProjectLevel);
  }

  private EntityDetail getEntityDetailWithMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("new", "true");
    EntityDetail entityDetailProjectLevel = EntityDetail.builder()
                                                .entityRef(IdentifierRef.builder()
                                                               .accountIdentifier(ACCOUNT_ID)
                                                               .orgIdentifier(ORG_ID)
                                                               .projectIdentifier(PROJECT_ID)
                                                               .identifier("id1")
                                                               .metadata(metadata)
                                                               .build())
                                                .type(EntityType.CONNECTORS)
                                                .build();
    return entityDetailProjectLevel;
  }

  private EntityDetail getEntityDetailWithoutMetadata() {
    EntityDetail entityDetailProjectLevel = EntityDetail.builder()
                                                .entityRef(IdentifierRef.builder()
                                                               .accountIdentifier(ACCOUNT_ID)
                                                               .orgIdentifier(ORG_ID)
                                                               .projectIdentifier(PROJECT_ID)
                                                               .identifier("id1")
                                                               .build())
                                                .type(EntityType.CONNECTORS)
                                                .build();
    return entityDetailProjectLevel;
  }
}