package io.harness.ceng.remote.resources;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.io.IOException;
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
  public void testBaseGetMethod() throws IOException {
    ResponseDTO<Boolean> responseDTO = ceNextGenServiceResource.test();
    assertThat(responseDTO.getStatus()).isEqualTo(Status.SUCCESS);
    assertThat(responseDTO.getData()).isTrue();
  }
}