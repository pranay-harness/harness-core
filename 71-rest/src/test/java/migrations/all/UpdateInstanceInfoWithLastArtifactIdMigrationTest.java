package migrations.all;

import static io.harness.rule.OwnerRule.ANSHUL;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACCOUNT_NAME;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.K8sContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.dl.WingsPersistence;

import java.util.HashMap;
import java.util.Map;

public class UpdateInstanceInfoWithLastArtifactIdMigrationTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @InjectMocks @Inject private UpdateInstanceInfoWithLastArtifactIdMigration instanceInfoMigration;

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testMigrate() {
    Account account = anAccount().withAccountName(ACCOUNT_NAME).withUuid(ACCOUNT_ID).build();
    wingsPersistence.save(account);

    Application application = anApplication().accountId(ACCOUNT_ID).uuid(APP_ID).appId(APP_ID).build();
    wingsPersistence.save(application);

    Instance instance1 = Instance.builder()
                             .uuid("instance1")
                             .lastArtifactStreamId(ARTIFACT_STREAM_ID + "1")
                             .appId(APP_ID)
                             .accountId(ACCOUNT_ID)
                             .build();
    Instance instance2 = Instance.builder()
                             .uuid("instance2")
                             .lastArtifactStreamId(ARTIFACT_STREAM_ID + "2")
                             .appId(APP_ID)
                             .accountId(ACCOUNT_ID)
                             .build();
    instance2.setInstanceInfo(
        K8sPodInfo.builder().containers(asList(K8sContainerInfo.builder().image("image:latest").build())).build());

    Instance instance3 = Instance.builder()
                             .uuid("instance3")
                             .lastArtifactStreamId(ARTIFACT_STREAM_ID + "3")
                             .appId(APP_ID)
                             .accountId(ACCOUNT_ID)
                             .isDeleted(true)
                             .build();

    wingsPersistence.save(instance1);
    wingsPersistence.save(instance2);
    wingsPersistence.save(instance3);

    Artifact artifact =
        anArtifact().withUuid(ARTIFACT_ID).withAppId(APP_ID).withArtifactStreamId(ARTIFACT_STREAM_ID + "2").build();
    wingsPersistence.save(artifact);

    instanceInfoMigration.migrate();
    Instance instance = wingsPersistence.get(Instance.class, "instance2");
    assertThat(instance).isNotNull();
    assertThat(instance.getLastArtifactId()).isNull();

    Map<String, String> metadata = new HashMap<>();
    metadata.put("image", "image:latest");
    artifact.setMetadata(metadata);
    wingsPersistence.save(artifact);

    instanceInfoMigration.migrate();
    instance = wingsPersistence.get(Instance.class, "instance2");
    assertThat(instance).isNotNull();
    assertThat(instance.getLastArtifactId()).isEqualTo(ARTIFACT_ID);
  }
}
