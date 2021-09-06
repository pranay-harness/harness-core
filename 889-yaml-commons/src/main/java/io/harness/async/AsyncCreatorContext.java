package io.harness.async;

import com.google.protobuf.ByteString;

public interface AsyncCreatorContext {
  ByteString getGitSyncBranchContext();
}
