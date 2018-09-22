package software.wings.beans.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.data.structure.EmptyPredicate;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapGroupConfig;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapGroupSettings implements LdapGroupConfig {
  @JsonProperty @NotBlank String baseDN;
  @JsonProperty @NotBlank String searchFilter = LdapConstants.DEFAULT_GROUP_SEARCH_FILTER;
  @JsonProperty @NotBlank String nameAttr = "cn";
  @JsonProperty @NotBlank String descriptionAttr = "description";
  @JsonProperty @NotBlank String userMembershipAttr = "member";
  @JsonProperty @NotBlank String referencedUserAttr = "dn";

  @JsonIgnore
  public String getFilter(String additionalFilter) {
    if (EmptyPredicate.isEmpty(additionalFilter)) {
      return String.format("%s", searchFilter);
    }
    return String.format("(&(cn=%s)%s)", additionalFilter, searchFilter);
  }

  @JsonIgnore
  @Override
  public String[] getReturnAttrs() {
    return new String[] {nameAttr, descriptionAttr};
  }
}
