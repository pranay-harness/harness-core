package software.wings.service.impl.ldap;

import static io.harness.rule.OwnerRule.VIKAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.helpers.ext.ldap.LdapConstants.GROUP_MEMBERS_EXCEEDED;
import static software.wings.helpers.ext.ldap.LdapConstants.GROUP_SIZE_ATTR;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.helpers.ext.ldap.LdapGroupConfig;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapDelegateServiceImplTest extends WingsBaseTest {
  static final String DESCRIPTION = "description";
  static final String DESCRIPTION_OF_GROUP = "Admin User Group";
  static final String NAME_OF_GROUP = "admin@harness.io";
  static final String GROUP_DN = "Admin User DN";
  static final String GROUP_SIZE_BELOW_LIMIT = "1000";
  static final String GROUP_SIZE_ABOVE_LIMIT = "1501";
  static final String NAME = "name";

  @Mock LdapEntry ldapEntry;
  @Mock LdapGroupConfig ldapGroupConfig;
  @Mock LdapAttribute nameLdapAttribute;
  @Mock LdapAttribute groupLdapAttribute;
  @Mock LdapAttribute descLdapAttribute;

  @Before
  public void setUp() {
    String[] attributes = new String[1];
    attributes[0] = DESCRIPTION;

    when(ldapGroupConfig.getDescriptionAttr()).thenReturn(DESCRIPTION);
    when(ldapGroupConfig.getNameAttr()).thenReturn(NAME);

    when(ldapEntry.getAttributeNames()).thenReturn(attributes);
    when(ldapEntry.getDn()).thenReturn(GROUP_DN);
    when(ldapEntry.getAttribute(GROUP_SIZE_ATTR)).thenReturn(groupLdapAttribute);
    when(ldapEntry.getAttribute(DESCRIPTION)).thenReturn(descLdapAttribute);
    when(descLdapAttribute.getStringValue()).thenReturn(DESCRIPTION_OF_GROUP);
    when(ldapEntry.getAttribute(NAME)).thenReturn(nameLdapAttribute);
    when(nameLdapAttribute.getStringValue()).thenReturn(NAME_OF_GROUP);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testBuildLdapGroupResponse_withGroupSizeWithinLimit() {
    when(groupLdapAttribute.getStringValue()).thenReturn(GROUP_SIZE_BELOW_LIMIT);
    LdapDelegateServiceImpl ldapDelegateService = new LdapDelegateServiceImpl();
    LdapGroupResponse ldapGroupResponse = ldapDelegateService.buildLdapGroupResponse(ldapEntry, ldapGroupConfig);
    assertThat(ldapGroupResponse).isNotNull();
    assertThat(ldapGroupResponse.getDescription()).isEqualTo(DESCRIPTION_OF_GROUP);
    assertThat(ldapGroupResponse.getDn()).isEqualTo(GROUP_DN);
    assertThat(ldapGroupResponse.getName()).isEqualTo(NAME_OF_GROUP);
    assertThat(ldapGroupResponse.getName()).isEqualTo(NAME_OF_GROUP);
    assertThat(ldapGroupResponse.getTotalMembers()).isEqualTo(Integer.parseInt(GROUP_SIZE_BELOW_LIMIT));
    assertThat(ldapGroupResponse.isSelectable()).isTrue();
    assertThat(ldapGroupResponse.getMessage()).isEmpty();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testBuildLdapGroupResponse_withGroupSizeAboveLimit() {
    when(groupLdapAttribute.getStringValue()).thenReturn(GROUP_SIZE_ABOVE_LIMIT);
    LdapDelegateServiceImpl ldapDelegateService = new LdapDelegateServiceImpl();
    LdapGroupResponse ldapGroupResponse = ldapDelegateService.buildLdapGroupResponse(ldapEntry, ldapGroupConfig);
    assertThat(ldapGroupResponse).isNotNull();
    assertThat(ldapGroupResponse.getDescription()).isEqualTo(DESCRIPTION_OF_GROUP);
    assertThat(ldapGroupResponse.getDn()).isEqualTo(GROUP_DN);
    assertThat(ldapGroupResponse.getName()).isEqualTo(NAME_OF_GROUP);
    assertThat(ldapGroupResponse.getName()).isEqualTo(NAME_OF_GROUP);
    assertThat(ldapGroupResponse.getTotalMembers()).isEqualTo(Integer.parseInt(GROUP_SIZE_ABOVE_LIMIT));
    assertThat(ldapGroupResponse.isSelectable()).isFalse();
    assertThat(ldapGroupResponse.getMessage()).isEqualTo(GROUP_MEMBERS_EXCEEDED);
  }
}
