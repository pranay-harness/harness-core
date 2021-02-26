package io.harness.accesscontrol.acl.models;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.HUserPrincipal;
import io.harness.persistence.PersistentEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "HACLKeys")
public class HACL extends ACL implements PersistentEntity {
  private static final String DELIMITER = "$";
  private static final String ACCOUNT_PREFIX = "account/%s";
  private static final String ORG_PREFIX = "$org/%s";
  private static final String PROJECT_PREFIX = "$project/%s";

  String permission;
  String resourceGroupIdentifier;
  HResource resource;
  HUserPrincipal principal;
  SourceMetadata sourceMetadata;
  ParentMetadata parentMetadata;
  String aclQueryString;

  public static String getAclQueryString(
      ParentMetadata parentMetadata, HResource resource, HUserPrincipal principal, String permission) {
    return String.format(ACCOUNT_PREFIX, parentMetadata.getAccountIdentifier())
        + (!isEmpty(parentMetadata.getOrgIdentifier()) ? String.format(ORG_PREFIX, parentMetadata.getOrgIdentifier())
                                                       : "")
        + (!isEmpty(parentMetadata.getProjectIdentifier())
                ? String.format(PROJECT_PREFIX, parentMetadata.getProjectIdentifier())
                : "")
        + DELIMITER + permission + DELIMITER + resource.getResourceType() + DELIMITER + resource.getResourceIdentifier()
        + DELIMITER + principal.getPrincipalType() + DELIMITER + principal.getPrincipalIdentifier();
  }
}
