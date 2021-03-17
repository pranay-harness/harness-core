package io.harness.accesscontrol.acl.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(builderClassName = "Builder")
public class ParentMetadata {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
}
