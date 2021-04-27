package io.harness.consumers;

import static io.harness.annotations.dev.HarnessTeam.PL;
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
import io.harness.accesscontrol.roles.persistence.RoleDBO;
import io.harness.aggregator.consumers.RoleChangeConsumerImpl;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class RoleChangeConsumerImplTest extends CategoryTest {
  private RoleChangeConsumerImpl roleChangeConsumer;
  private ACLService aclService;

  @Before
  public void setup() {
    aclService = mock(ACLService.class);
    roleChangeConsumer = new RoleChangeConsumerImpl(aclService);
  }

  private RoleDBO getRoleWithPermissionsChanged() {
    return RoleDBO.builder().id("some_random_id").permissions(Sets.newHashSet("a.b.c", "x.y.z")).build();
  }

  private RoleDBO getRoleWithPermissionsNotChanged() {
    return RoleDBO.builder()
        .id("some_random_id")
        .description("a new description")
        .tags(ImmutableMap.of("x", "y"))
        .build();
  }

  public List<ACL> getAlreadyExistingACLS() {
    return Lists.newArrayList(ImmutableList.of(
        ACL.builder()
            .roleAssignmentId("roleAssignmentId1")
            .principalType("USER")
            .sourceMetadata(SourceMetadata.builder().roleAssignmentIdentifier("roleAssignmentIdentifier").build())
            .scopeIdentifier("/ACCOUNT/account/ORG/org")
            .principalIdentifier("qwerty")
            .permissionIdentifier("core_secret_edit")
            .resourceSelector("/SECRETS/abcde")
            .build()));
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleUpdateWithPermissionsChanged() {
    when(aclService.getByRole(anyString(), anyString(), anyBoolean())).thenReturn(getAlreadyExistingACLS());
    doNothing().when(aclService).deleteAll(any());
    when(aclService.saveAll(anyList())).thenReturn(2L);
    long count = roleChangeConsumer.consumeUpdateEvent("some_random_id", getRoleWithPermissionsChanged());

    Assertions.assertThat(count).isEqualTo(2);
    verify(aclService).saveAll(any());
    verify(aclService).deleteAll(any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testRoleUpdateWithPermissionsNotChanged() {
    long count = roleChangeConsumer.consumeUpdateEvent("some_random_id", getRoleWithPermissionsNotChanged());
    Assertions.assertThat(count).isEqualTo(0);
  }
}
