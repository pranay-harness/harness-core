package software.wings.beans.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapUserConfig;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserSettings implements LdapUserConfig {
  @JsonProperty @NotBlank String baseDN;
  @JsonProperty @NotBlank String searchFilter = LdapConstants.DEFAULT_USER_SEARCH_FILTER;
  @JsonProperty @NotBlank String emailAttr = "email";
  @JsonProperty @NotBlank String displayNameAttr = "cn";
  @JsonProperty @NotBlank String groupMembershipAttr = "memberOf";

  @JsonIgnore
  @Override
  public String getUserFilter() {
    return String.format("(&(%s={user})%s)", emailAttr, searchFilter);
  }

  @JsonIgnore
  @Override
  public String getLoadUsersFilter() {
    return String.format("(&%s(%s=*))", searchFilter, emailAttr);
  }

  @JsonIgnore
  @Override
  public String getGroupMembershipFilter(String groupDn) {
    return String.format("(&%s(%s:%s:=%s)(%s=*))", searchFilter, groupMembershipAttr,
        LdapConstants.LDAP_MATCHING_RULE_IN_CHAIN, groupDn, emailAttr);
  }

  @JsonIgnore
  @Override
  public String[] getReturnAttrs() {
    return new String[] {emailAttr, displayNameAttr};
  }
}
