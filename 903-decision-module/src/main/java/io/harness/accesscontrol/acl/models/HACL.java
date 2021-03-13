package io.harness.accesscontrol.acl.models;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.accesscontrol.Principal;
import io.harness.persistence.PersistentEntity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "HACLKeys")
public class HACL implements PersistentEntity {
  private static final String DELIMITER = "$";
  private static final String ACCOUNT_PREFIX = "account/%s";
  private static final String ORG_PREFIX = "$org/%s";
  private static final String PROJECT_PREFIX = "$project/%s";

  String permission;
  String resourceGroupIdentifier;
  HResource resource;
  Principal principal;
  ParentMetadata parentMetadata;
  String scopeIdentifier;
  String aclQueryString;

  public static String getAclQueryString(
      ParentMetadata parentMetadata, HResource resource, Principal principal, String permission) {
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
