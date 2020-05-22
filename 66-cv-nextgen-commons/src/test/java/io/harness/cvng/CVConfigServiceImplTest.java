package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.models.CVConfig;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVConfigServiceImplTest extends CVNextGenBaseTest {
  @Inject CVConfigService cvConfigService;

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave() {
    CVConfig cvConfig = CVConfig.builder().categoryId(generateUuid()).build();
    CVConfig updated = cvConfigService.save(generateUuid(), cvConfig);
    CVConfig saved = cvConfigService.get(updated.getUuid());
    assertThat(saved.getCategoryId()).isEqualTo(cvConfig.getCategoryId());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGet() {
    CVConfig cvConfig = CVConfig.builder().categoryId(generateUuid()).build();
    CVConfig updated = cvConfigService.save(generateUuid(), cvConfig);
    CVConfig saved = cvConfigService.get(updated.getUuid());
    assertThat(saved.getCategoryId()).isEqualTo(cvConfig.getCategoryId());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete() {
    CVConfig cvConfig = CVConfig.builder().categoryId(generateUuid()).build();
    CVConfig updated = cvConfigService.save(generateUuid(), cvConfig);
    cvConfigService.delete(updated.getUuid());
    assertThat(cvConfigService.get(cvConfig.getUuid())).isEqualTo(null);
  }
}