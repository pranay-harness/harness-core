package io.harness.consumers;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.accesscontrol.acl.persistence.ACL;
import io.harness.accesscontrol.acl.persistence.SourceMetadata;
import io.harness.accesscontrol.acl.persistence.repositories.ACLRepository;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.aggregator.consumers.ChangeConsumerService;
import io.harness.aggregator.consumers.RoleAssignmentCRUDEventHandler;
import io.harness.aggregator.consumers.RoleAssignmentChangeConsumerImpl;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class RoleAssignmentChangeConsumerImplTest extends CategoryTest {
  private RoleAssignmentChangeConsumerImpl roleAssignmentChangeConsumer;
  private ACLRepository aclRepository;
  private RoleAssignmentRepository roleAssignmentRepository;
  private ChangeConsumerService changeConsumerService;
  private RoleAssignmentCRUDEventHandler roleAssignmentCRUDEventHandler;

  @Before
  public void setup() {
    aclRepository = mock(ACLRepository.class);
    roleAssignmentRepository = mock(RoleAssignmentRepository.class);
    changeConsumerService = mock(ChangeConsumerService.class);
    roleAssignmentCRUDEventHandler = mock(RoleAssignmentCRUDEventHandler.class);

    roleAssignmentChangeConsumer = new RoleAssignmentChangeConsumerImpl(
        aclRepository, roleAssignmentRepository, changeConsumerService, roleAssignmentCRUDEventHandler);
  }

  private List<ACL> getAlreadyExistingACLS() {
    return Lists.newArrayList(
        ImmutableList.of(ACL.builder()
                             .roleAssignmentId("roleAssignmentId1")
                             .principalType("USER")
                             .sourceMetadata(SourceMetadata.builder()
                                                 .roleAssignmentIdentifier("roleAssignmentIdentifier")
                                                 .resourceGroupIdentifier("resourceGroupIdentifier")
                                                 .build())
                             .scopeIdentifier("/ACCOUNT/account/ORG/org")
                             .principalIdentifier("qwerty")
                             .permissionIdentifier("core_secret_edit")
                             .resourceSelector("/SECRETS/abcde")
                             .build()));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentCreation() {
    //    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().principalType(PrincipalType.USER).build();
    //    when(roleService.get(any(), any(), any()))
    //        .thenReturn(
    //            Optional.of(Role.builder()
    //                            .permissions(Sets.newHashSet(ImmutableList.of("core_secret_create",
    //                            "core_secret_edit"))) .build()));
    //    when(resourceGroupService.get(any(), any()))
    //        .thenReturn(Optional.of(ResourceGroup.builder()
    //                                    .resourceSelectors(Sets.newHashSet(ImmutableList.of("/SECRET/abc",
    //                                    "/SECRET/xyz"))) .build()));
    //    when(aclService.saveAll(any())).thenReturn(10L);
    //    long count = roleAssignmentChangeConsumer.consumeCreateEvent(SOME_RANDOM_ID, roleAssignmentDBO);
    //    assertThat(count).isEqualTo(10L);
    //    verify(roleService).get(any(), any(), any());
    //    verify(resourceGroupService).get(any(), any());
    //    verify(aclService).saveAll(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentCreationWhenRoleNotFound() {
    //    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().build();
    //    when(roleService.get(any(), any(), any())).thenReturn(Optional.empty());
    //    long count = roleAssignmentChangeConsumer.consumeCreateEvent(SOME_RANDOM_ID, roleAssignmentDBO);
    //    assertThat(count).isEqualTo(0L);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentCreationWhenResourceGroupNotFound() {
    //    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().build();
    //    when(roleService.get(any(), any(), any()))
    //        .thenReturn(
    //            Optional.of(Role.builder()
    //                            .permissions(Sets.newHashSet(ImmutableList.of("core_secret_create",
    //                            "core_secret_edit"))) .build()));
    //    when(resourceGroupService.get(any(), any())).thenReturn(Optional.empty());
    //    long count = roleAssignmentChangeConsumer.consumeCreateEvent(SOME_RANDOM_ID, roleAssignmentDBO);
    //    assertThat(count).isEqualTo(0L);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentUpdationWithRelevantFieldsChanged() {
    //    RoleAssignmentDBO roleAssignmentDBO = RoleAssignmentDBO.builder().id("xyz").disabled(true).build();
    //    when(aclService.getByRoleAssignment("xyz")).thenReturn(getAlreadyExistingACLS());
    //    when(aclService.saveAll(anyList())).thenReturn(1L);
    //    long count = roleAssignmentChangeConsumer.consumeUpdateEvent("xyz", roleAssignmentDBO);
    //    assertThat(count).isEqualTo(1L);
    //    verify(aclService).getByRoleAssignment(any());
    //    verify(aclService).saveAll(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentUpdationWithRelevantFieldsNotChanged() {
    //    long count = roleAssignmentChangeConsumer.consumeUpdateEvent(
    //        SOME_RANDOM_ID, RoleAssignmentDBO.builder().version(1L).build());
    //    assertThat(count).isEqualTo(0L);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleAssignmentDeletion() {
    //    when(aclService.deleteByRoleAssignment(any())).thenReturn(100L);
    //    long count = roleAssignmentChangeConsumer.consumeDeleteEvent(SOME_RANDOM_ID);
    //    assertThat(count).isEqualTo(100L);
    //    verify(aclService).deleteByRoleAssignment(any());
  }
}
