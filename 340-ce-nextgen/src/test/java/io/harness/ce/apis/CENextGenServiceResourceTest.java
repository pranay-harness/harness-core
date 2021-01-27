package io.harness.ce.apis;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ceng.apis.CENextGenServiceResource;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CENextGenServiceResourceTest extends CategoryTest {
  @InjectMocks CENextGenServiceResource ceNextGenServiceResource;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.UTSAV)
  @Category(UnitTests.class)
  public void testHealth() {
    ResponseDTO<Boolean> responseDTO = ceNextGenServiceResource.health();
    assertThat(responseDTO.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(responseDTO.getData()).isTrue();
  }
}