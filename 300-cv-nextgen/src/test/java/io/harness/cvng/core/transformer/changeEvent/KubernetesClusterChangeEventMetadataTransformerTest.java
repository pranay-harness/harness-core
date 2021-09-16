package io.harness.cvng.core.transformer.changeEvent;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.KubernetesClusterActivity;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventMetadata;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class KubernetesClusterChangeEventMetadataTransformerTest {
  KubernetesClusterChangeEventMetadataTransformer transformer;
  BuilderFactory builderFactory;

  @Before
  public void setup() {
    transformer = new KubernetesClusterChangeEventMetadataTransformer();
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetEntity() {
    ChangeEventDTO changeEventDTO = builderFactory.getKubernetesClusterChangeEventDTOBuilder().build();
    KubernetesClusterActivity activity = transformer.getEntity(changeEventDTO);
    verifyEqual(activity, changeEventDTO);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetMetadata() {
    KubernetesClusterActivity activity = builderFactory.getKubernetesClusterActivityBuilder().build();
    ChangeEventDTO changeEventDTO = transformer.getDTO(activity);
    verifyEqual(activity, changeEventDTO);
  }

  private void verifyEqual(KubernetesClusterActivity activity, ChangeEventDTO changeEventDTO) {
    KubernetesChangeEventMetadata metadata = (KubernetesChangeEventMetadata) changeEventDTO.getChangeEventMetaData();
    assertThat(activity.getAccountId()).isEqualTo(changeEventDTO.getAccountId());
    assertThat(activity.getOrgIdentifier()).isEqualTo(changeEventDTO.getOrgIdentifier());
    assertThat(activity.getProjectIdentifier()).isEqualTo(changeEventDTO.getProjectIdentifier());
    assertThat(activity.getEventTime().toEpochMilli()).isEqualTo(changeEventDTO.getEventTime());
    assertThat(activity.getType()).isEqualTo(changeEventDTO.getType().getActivityType());
    assertThat(activity.getAction().name()).isEqualTo(metadata.getAction().name());
    assertThat(activity.getResourceType().name()).isEqualTo(metadata.getResourceType().name());

    assertThat(activity.getNamespace()).isEqualTo(metadata.getNamespace());
    assertThat(activity.getWorkload()).isEqualTo(metadata.getWorkload());
    assertThat(activity.getOldYaml()).isEqualTo(metadata.getOldYaml());
    assertThat(activity.getNewYaml()).isEqualTo(metadata.getNewYaml());
  }
}
