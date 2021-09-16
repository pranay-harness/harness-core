package io.harness.pms.expressions.functors;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ngpipeline.artifact.bean.ArtifactsOutcome;
import io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome;
import io.harness.ngpipeline.artifact.bean.SidecarsOutcome;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class SidecarImagePullSecretFunctorTest extends CategoryTest {
  private static String artifactIdentifier = "artifactId";
  @Mock ImagePullSecretUtils imagePullSecretUtils;
  ArtifactsOutcome artifactsOutcome = ArtifactsOutcome.builder().build();
  SidecarsOutcome sidecarsOutcome = new SidecarsOutcome();
  ArtifactsOutcome artifactsOutcome1 = ArtifactsOutcome.builder().sidecars(sidecarsOutcome).build();
  @InjectMocks SidecarImagePullSecretFunctor sidecarImagePullSecretFunctor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGet() {
    assertNull(sidecarImagePullSecretFunctor.get(artifactIdentifier));
    on(sidecarImagePullSecretFunctor).set("artifactsOutcome", artifactsOutcome);
    assertNull(sidecarImagePullSecretFunctor.get(artifactIdentifier));
    on(sidecarImagePullSecretFunctor).set("artifactsOutcome", artifactsOutcome1);
    assertNull(sidecarImagePullSecretFunctor.get(artifactIdentifier));
    artifactsOutcome1.getSidecars().put("artifactId", DockerArtifactOutcome.builder().build());
    when(imagePullSecretUtils.getImagePullSecret(any(), any())).thenReturn("ImagePullSecret");
    assertEquals(sidecarImagePullSecretFunctor.get(artifactIdentifier), "ImagePullSecret");
  }
}
