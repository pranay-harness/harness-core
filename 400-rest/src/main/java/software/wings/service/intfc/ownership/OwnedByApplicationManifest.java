package software.wings.service.intfc.ownership;

public interface OwnedByApplicationManifest {
  void pruneByApplicationManifest(String appId, String applicationManifestId);
}
