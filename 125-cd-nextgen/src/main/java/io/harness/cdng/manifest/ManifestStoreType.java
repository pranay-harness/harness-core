/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.manifest;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public interface ManifestStoreType {
  String GIT = "Git";
  String LOCAL = "Local";
  String GITHUB = "Github";
  String BITBUCKET = "Bitbucket";
  String GITLAB = "GitLab";
  String HTTP = "Http";
  String S3 = "S3";
  String GCS = "Gcs";

  static boolean isInGitSubset(String manifestType) {
    switch (manifestType) {
      case GIT:
      case GITHUB:
      case BITBUCKET:
      case GITLAB:
        return true;

      default:
        return false;
    }
  }

  static boolean isInStorageRepository(String manifestType) {
    switch (manifestType) {
      case HTTP:
      case S3:
      case GCS:
        return true;

      default:
        return false;
    }
  }
}
