package io.harness.cdng.manifest;

public interface ManifestStoreType {
  String GIT = "Git";
  String LOCAL = "Local";
  String GITHUB = "Github";
  String BITBUCKET = "Bitbucket";
  String GITLAB = "GitLab";
  String HTTP = "Http";

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
}
