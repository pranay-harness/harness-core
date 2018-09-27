package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import migrations.Migration;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.beans.artifact.GcsArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.gcs.GcsService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import java.util.List;

public class GcsArtifactProjectIdMigration implements Migration {
  private static Logger logger = LoggerFactory.getLogger(GcsArtifactProjectIdMigration.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private GcsService gcsService;

  private List<EncryptedDataDetail> getEncryptedDataDetails(Encryptable settingValue) {
    return secretManager.getEncryptionDetails(settingValue, null, null);
  }

  @Override
  public void migrate() {
    Query<ArtifactStream> query =
        wingsPersistence.createQuery(ArtifactStream.class).filter("artifactStreamType", ArtifactStreamType.GCS.name());
    try (HIterator<ArtifactStream> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        GcsArtifactStream artifactStream = (GcsArtifactStream) records.next();

        if (artifactStream != null && isNotEmpty(artifactStream.getSettingId())
            && isEmpty(artifactStream.getProjectId())) {
          SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
          if (settingAttribute == null) {
            logger.info("GCP Cloud provider Settings Attribute is null. Can not set Project Id in Artifact Stream");
            continue;
          } else {
            // Get project Id from GCP Config and service account
            SettingValue settingValue = settingAttribute.getValue();
            List<EncryptedDataDetail> encryptedDataDetails = getEncryptedDataDetails((Encryptable) settingValue);
            GcpConfig gcpConfig = (GcpConfig) settingValue;
            String projectId = gcsService.getProject(gcpConfig, encryptedDataDetails);

            // Set the project Id for artifact stream
            if (isNotEmpty(projectId)) {
              artifactStream.setProjectId(projectId);
              wingsPersistence.save(artifactStream);
            }
          }
        }
      }
    }
  }
}