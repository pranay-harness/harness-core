package io.harness.argo.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppStatus {
  private Health health;
  private CurrentSyncDetails currentSyncDetails;
  private LastSyncDetails lastSyncDetails;

  public enum Health {
    SUSPENDED,
    PROGRESSING,
    HEALTHY,
    UNHEALTHY;
  }

  @Data
  @Builder
  public static class CurrentSyncDetails {
    private GitSyncStatus status;
    private DeployedMetadata deployedMetadata;
  }

  @Data
  @Builder
  public static class LastSyncDetails {
    private LastSyncStatus status;
    private DeployedMetadata deployedMetadata;
  }

  @Data
  @Builder
  public static class DeployedMetadata {
    private String revision;
    private String author;
    private String comment;
  }

  public enum GitSyncStatus { OUT_OF_SYNC, IN_SYNC }

  public enum LastSyncStatus { SUCCESS, FAILED }

  public static AppStatus fromArgoApp(ArgoApp argoApp, RevisionMeta incomingRevision, RevisionMeta syncedRevision) {
    final Health health = getHealth(argoApp);
    final GitSyncStatus syncStatus =
        "OutOfSync".equalsIgnoreCase(argoApp.syncStatus()) ? GitSyncStatus.OUT_OF_SYNC : GitSyncStatus.IN_SYNC;
    final LastSyncStatus lastSyncStatus =
        "Succeeded".equalsIgnoreCase(argoApp.previousSyncStatus()) ? LastSyncStatus.SUCCESS : LastSyncStatus.FAILED;
    final DeployedMetadata incomingMetadata = DeployedMetadata.builder()
                                                  .revision(argoApp.incomingRevision())
                                                  .author(incomingRevision.getAuthor())
                                                  .comment(incomingRevision.getMessage())
                                                  .build();
    final DeployedMetadata deployedMetadata = DeployedMetadata.builder()
                                                  .revision(argoApp.syncedRevision())
                                                  .author(syncedRevision.getAuthor())
                                                  .comment(syncedRevision.getMessage())
                                                  .build();
    return AppStatus.builder()
        .health(health)
        .currentSyncDetails(CurrentSyncDetails.builder().status(syncStatus).deployedMetadata(incomingMetadata).build())
        .lastSyncDetails(LastSyncDetails.builder().status(lastSyncStatus).deployedMetadata(deployedMetadata).build())
        .build();
  }

  private static Health getHealth(ArgoApp argoApp) {
    if ("healthy".equalsIgnoreCase(argoApp.health()))
      return Health.HEALTHY;
    else if ("Progressing".equalsIgnoreCase(argoApp.health()))
      return Health.PROGRESSING;
    else if ("Suspended".equalsIgnoreCase(argoApp.health()))
      return Health.SUSPENDED;
    return Health.UNHEALTHY;
  }
}
