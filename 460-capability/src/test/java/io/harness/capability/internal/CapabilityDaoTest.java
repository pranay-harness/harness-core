package io.harness.capability.internal;

import static io.harness.rule.OwnerRule.MATT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CapabilityTestBase;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CapabilityDaoTest extends CapabilityTestBase {
  @InjectMocks @Inject private CapabilityDao capabilityDao;

  private final String ACCOUNT_ID = "test-account-id";
  private final String DELEGATE_ID_1 = "delegate-id-1";
  private final String DELEGATE_ID_2 = "delegate-id-2";
  private final String DELEGATE_ID_3 = "delegate-id-3";
  private final String CAPABILITY_ID_1 = "capability-id-1";
  private final String CAPABILITY_ID_2 = "capability-id-2";

  @Before
  public void setUpCapabilities() {
    capabilityDao.addCapabilityRequirement(CapabilityRequirement.builder()
                                               .accountId(ACCOUNT_ID)
                                               .uuid(CAPABILITY_ID_1)
                                               .capabilityParameters(CapabilityParameters.getDefaultInstance())
                                               .build(),
        Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2, DELEGATE_ID_3));
    capabilityDao.addCapabilityRequirement(CapabilityRequirement.builder()
                                               .accountId(ACCOUNT_ID)
                                               .uuid(CAPABILITY_ID_2)
                                               .capabilityParameters(CapabilityParameters.getDefaultInstance())
                                               .build(),
        Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2, DELEGATE_ID_3));
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testInitializedCapabilities() {
    assertThat(capabilityDao.getAllCapabilityRequirement(ACCOUNT_ID)).hasSize(2);
    List<CapabilitySubjectPermission> subjectPermissions =
        capabilityDao.getAllDelegatePermission(ACCOUNT_ID, Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2, DELEGATE_ID_3));
    assertThat(subjectPermissions).hasSize(6);
    for (CapabilitySubjectPermission permission : subjectPermissions) {
      assertThat(permission.getPermissionResult()).isEqualTo(PermissionResult.UNCHECKED);
    }
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testValidateCapabilities() {
    capabilityDao.updateCapabilityPermission(
        ACCOUNT_ID, CAPABILITY_ID_1, Arrays.asList(DELEGATE_ID_1, DELEGATE_ID_2), PermissionResult.ALLOWED);
    List<CapabilitySubjectPermission> subjectPermissions =
        capabilityDao.getAllCapabilityPermission(ACCOUNT_ID, Arrays.asList(CAPABILITY_ID_1));
    CapabilitySubjectPermission permission1 = CapabilitySubjectPermission.builder().build();
    CapabilitySubjectPermission permission3 = CapabilitySubjectPermission.builder().build();
    for (CapabilitySubjectPermission permission : subjectPermissions) {
      if (permission.getDelegateId().equals(DELEGATE_ID_1)) {
        permission1 = permission;
      }
      if (permission.getDelegateId().equals(DELEGATE_ID_3)) {
        permission3 = permission;
      }
    }
    assertThat(permission1.getValidUntil().toInstant().toEpochMilli())
        .isGreaterThan(permission3.getValidUntil().toInstant().toEpochMilli());
    assertThat(permission1.getPermissionResult()).isEqualTo(PermissionResult.ALLOWED);
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testUpdateRequirement() {
    CapabilityRequirement testedRequirement = CapabilityRequirement.builder().build();
    for (CapabilityRequirement requirement : capabilityDao.getAllCapabilityRequirement(ACCOUNT_ID)) {
      if (requirement.getUuid().equals(CAPABILITY_ID_1)) {
        testedRequirement = requirement;
        break;
      }
    }
    Instant validUntil = testedRequirement.getValidUntil().toInstant();
    capabilityDao.updateCapabilityRequirement(ACCOUNT_ID, CAPABILITY_ID_1);

    for (CapabilityRequirement requirement : capabilityDao.getAllCapabilityRequirement(ACCOUNT_ID)) {
      if (requirement.getUuid().equals(CAPABILITY_ID_1)) {
        testedRequirement = requirement;
        break;
      }
    }
    assertThat(testedRequirement.getValidUntil().toInstant().toEpochMilli()).isGreaterThan(validUntil.toEpochMilli());
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void testRemoveCapability() {
    capabilityDao.removeCapabilityRequirement(ACCOUNT_ID, CAPABILITY_ID_1);

    List<CapabilityRequirement> capabilityRequirements = capabilityDao.getAllCapabilityRequirement(ACCOUNT_ID);
    assertThat(capabilityRequirements.get(0).getUuid()).isEqualTo(CAPABILITY_ID_2);

    List<CapabilitySubjectPermission> capabilityPermissions =
        capabilityDao.getAllCapabilityPermission(ACCOUNT_ID, Arrays.asList(CAPABILITY_ID_1, CAPABILITY_ID_2));
    assertThat(capabilityPermissions).hasSize(3);
    for (CapabilitySubjectPermission permission : capabilityPermissions) {
      assertThat(permission.getCapabilityId()).isEqualTo(CAPABILITY_ID_2);
    }
  }
}
