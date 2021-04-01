package io.harness.delegate.terraform;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.terraform.TerraformBaseHelperImpl;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class TerraformBaseHelperImplTest extends CategoryTest {
  @InjectMocks @Inject TerraformBaseHelperImpl terraformBaseHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testParseOutput() {
    String workspaceCommandOutput = "* w1\n w2\n w3";
    assertThat(Arrays.asList("w1", "w2", "w3").equals(terraformBaseHelper.parseOutput(workspaceCommandOutput)))
        .isTrue();
  }
}
