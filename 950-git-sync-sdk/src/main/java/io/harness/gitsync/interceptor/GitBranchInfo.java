package io.harness.gitsync.interceptor;

import io.harness.encryption.SecretReference;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitBranchInfoKeys")
public class GitBranchInfo {
  String branch;
  String repo;
  // todo(abhinav): convert to different interfaces depending on how github connector comes out.
  @SecretReference String password;
  // todo(abhinav): See if we need to convert this to .
  String connectorFQN;
}
