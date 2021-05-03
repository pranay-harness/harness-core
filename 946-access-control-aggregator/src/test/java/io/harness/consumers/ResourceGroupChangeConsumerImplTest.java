package io.harness.consumers;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.mockito.Mockito.mock;

import io.harness.CategoryTest;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.SourceMetadata;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.roleassignments.persistence.RoleAssignmentDBO;
import io.harness.accesscontrol.roleassignments.persistence.repositories.RoleAssignmentRepository;
import io.harness.aggregator.consumers.ChangeConsumer;
import io.harness.aggregator.consumers.ResourceGroupChangeConsumerImpl;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class ResourceGroupChangeConsumerImplTest extends CategoryTest {
  private ResourceGroupChangeConsumerImpl resourceGroupChangeConsumer;
  private ACLRepository aclRepository;
  private RoleAssignmentRepository roleAssignmentRepository;
  private ChangeConsumer<RoleAssignmentDBO> roleAssignmentChangeConsumer;

  @Before
  public void setup() {
    aclRepository = mock(ACLRepository.class);
    roleAssignmentRepository = mock(RoleAssignmentRepository.class);
    roleAssignmentChangeConsumer = mock(ChangeConsumer.class);
    resourceGroupChangeConsumer =
        new ResourceGroupChangeConsumerImpl(aclRepository, roleAssignmentRepository, roleAssignmentChangeConsumer);
  }

  private ResourceGroupDBO getResourceGroupWithResourceSelectorsChanged() {
    return ResourceGroupDBO.builder()
        .id("some_random_id")
        .resourceSelectors(Sets.newHashSet("/SECRETS/*", "/PROJECTS/abcde"))
        .build();
  }

  private ResourceGroupDBO getResourceGroupWithResourceSelectorsNotChanged() {
    return ResourceGroupDBO.builder().id("some_random_id").version(1L).build();
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
  public void testResourceGroupUpdateWithPermissionsChanged() {
    //    when(aclService.getByResourceGroup(anyString(), anyString(),
    //    anyBoolean())).thenReturn(getAlreadyExistingACLS()); doNothing().when(aclService).deleteAll(any());
    //    when(aclService.saveAll(anyList())).thenReturn(2L);
    //    long count = resourceGroupChangeConsumer.consumeUpdateEvent(
    //        "some_random_id", getResourceGroupWithResourceSelectorsChanged());
    //
    //    Assertions.assertThat(count).isEqualTo(2);
    //    verify(aclService).saveAll(any());
    //    verify(aclService).deleteAll(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleUpdateWithPermissionsNotChanged() {
    //    long count = resourceGroupChangeConsumer.consumeUpdateEvent(
    //        "some_random_id", getResourceGroupWithResourceSelectorsNotChanged());
    //    Assertions.assertThat(count).isEqualTo(0);
  }
}
