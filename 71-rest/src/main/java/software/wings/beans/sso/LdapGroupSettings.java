package software.wings.beans.sso;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.structure.EmptyPredicate;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.helpers.ext.ldap.LdapGroupConfig;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapGroupSettings implements LdapGroupConfig {
  @SchemaIgnore static final Logger logger = LoggerFactory.getLogger(LdapGroupSettings.class);
  @JsonProperty @NotBlank String baseDN;
  @JsonProperty @NotBlank String searchFilter = LdapConstants.DEFAULT_GROUP_SEARCH_FILTER;
  @JsonProperty @NotBlank String nameAttr = "cn";
  @JsonProperty @NotBlank String descriptionAttr = "description";
  @JsonProperty @NotBlank String userMembershipAttr = "member";
  @JsonProperty @NotBlank String referencedUserAttr = "dn";

  @JsonIgnore
  public String getFilter(String additionalFilter) {
    String filterString;
    if (EmptyPredicate.isEmpty(additionalFilter)) {
      filterString = String.format("%s", searchFilter);
    } else {
      filterString = String.format("(&(cn=%s)%s)", additionalFilter, searchFilter);
    }
    logger.info("LdapGroupSettings filter is {}", filterString);
    return filterString;
  }

  @JsonIgnore
  @Override
  public String[] getReturnAttrs() {
    return new String[] {nameAttr, descriptionAttr};
  }
}
