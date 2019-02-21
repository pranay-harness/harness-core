package software.wings.beans.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapUserConfig;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserSettings implements LdapUserConfig {
  @SchemaIgnore static final Logger logger = LoggerFactory.getLogger(LdapUserSettings.class);
  @JsonProperty @NotBlank String baseDN;
  @JsonProperty @NotBlank String searchFilter = LdapConstants.DEFAULT_USER_SEARCH_FILTER;
  @JsonProperty @NotBlank String emailAttr = "mail";
  @JsonProperty @NotBlank String displayNameAttr = "cn";
  @JsonProperty @NotBlank String groupMembershipAttr = "memberOf";

  @JsonIgnore
  @Override
  public String getUserFilter() {
    String userFilter = String.format("(&(%s={user})%s)", emailAttr, searchFilter);
    logger.info("LDAP UserFilter is {}", userFilter);
    return userFilter;
  }

  @JsonIgnore
  @Override
  public String getLoadUsersFilter() {
    String loadUserFilter = String.format("(&%s(%s=*))", searchFilter, emailAttr);
    logger.info("LDAP loadUserFilter is {}", loadUserFilter);
    return loadUserFilter;
  }

  @JsonIgnore
  @Override
  public String getGroupMembershipFilter(String groupDn) {
    String groupMembershipFilter = String.format("(&%s(%s:%s:=%s)(%s=*))", searchFilter, groupMembershipAttr,
        LdapConstants.LDAP_MATCHING_RULE_IN_CHAIN, groupDn, emailAttr);
    logger.info("LDAP groupMembershipFilter is {}", groupMembershipFilter);
    return groupMembershipFilter;
  }

  @JsonIgnore
  @Override
  public String getFallbackGroupMembershipFilter(String groupDn) {
    String fallBackGroupMembershipFilter = String.format("(&%s(%s=%s))", searchFilter, groupMembershipAttr, groupDn);
    logger.info("LDAP fallBackGroupMembershipFilter is {}", fallBackGroupMembershipFilter);
    return fallBackGroupMembershipFilter;
  }

  @JsonIgnore
  @Override
  public String[] getReturnAttrs() {
    return new String[] {emailAttr, displayNameAttr};
  }
}
