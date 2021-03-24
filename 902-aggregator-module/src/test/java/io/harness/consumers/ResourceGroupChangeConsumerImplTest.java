package io.harness.consumers;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.SourceMetadata;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.aggregator.consumers.ResourceGroupChangeConsumerImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ResourceGroupChangeConsumerImplTest extends CategoryTest {
  private ResourceGroupChangeConsumerImpl resourceGroupChangeConsumer;
  private ACLService aclService;
  private ResourceGroupService resourceGroupService;
  private RoleService roleService;

  @Before
  public void setup() {
    aclService = mock(ACLService.class);
    resourceGroupService = mock(ResourceGroupService.class);
    roleService = mock(RoleService.class);
    resourceGroupChangeConsumer = new ResourceGroupChangeConsumerImpl(aclService, roleService, resourceGroupService);
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

  public List<ACL> getAlreadyExistingACLS() {
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
                             .permissionIdentifier("core.secret.edit")
                             .resourceSelector("/SECRETS/abcde")
                             .build()));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testResourceGroupUpdateWithPermissionsChanged() {
    when(aclService.getByResourceGroup(anyString(), anyString(), anyBoolean())).thenReturn(getAlreadyExistingACLS());
    doNothing().when(aclService).deleteAll(any());
    when(aclService.insertAllIgnoringDuplicates(anyList())).thenReturn(2L);
    long count = resourceGroupChangeConsumer.consumeUpdateEvent(
        "some_random_id", getResourceGroupWithResourceSelectorsChanged());

    Assertions.assertThat(count).isEqualTo(2);
    verify(aclService).insertAllIgnoringDuplicates(any());
    verify(aclService).deleteAll(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleUpdateWithPermissionsNotChanged() {
    long count = resourceGroupChangeConsumer.consumeUpdateEvent(
        "some_random_id", getResourceGroupWithResourceSelectorsNotChanged());
    Assertions.assertThat(count).isEqualTo(0);
  }
}
