package io.harness.pms.expressions.functors;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.pms.expressions.utils.ImagePullSecretUtils;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class ImagePullSecretFunctorTest extends CategoryTest {
  @Mock ImagePullSecretUtils imagePullSecretUtils;
  @Mock PmsOutcomeService pmsOutcomeService;
  @InjectMocks ImagePullSecretFunctor imagePullSecretFunctor;
  static String outcomeJson =
      "{\"__recast\":\"io.harness.ngpipeline.artifact.bean.ArtifactsOutcome\",\"primary\":{\"__recast\":\"io.harness.ngpipeline.artifact.bean.DockerArtifactOutcome\",\"connectorRef\":null,\"imagePath\":null,\"tag\":null,\"tagRegex\":null,\"identifier\":null,\"type\":null,\"primaryArtifact\":false,\"image\":null,\"imagePullSecret\":null},\"sidecars\":null}";
  static String outcomeNullPrimaryJson =
      "{\"__recast\":\"io.harness.ngpipeline.artifact.bean.ArtifactsOutcome\",\"sidecars\":null}";
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testGet() {
    when(pmsOutcomeService.resolve(any(), any())).thenReturn(outcomeJson);
    when(imagePullSecretUtils.getImagePullSecret(any(), any())).thenReturn("ImagePullSecret");
    assertNotNull(imagePullSecretFunctor.get("primary"));
    assertNotNull(imagePullSecretFunctor.get("sidecars"));
    assertNull(imagePullSecretFunctor.get("invalid"));
    when(pmsOutcomeService.resolve(any(), any())).thenReturn(outcomeNullPrimaryJson);
    assertNull(imagePullSecretFunctor.get("primary"));
  }
}
