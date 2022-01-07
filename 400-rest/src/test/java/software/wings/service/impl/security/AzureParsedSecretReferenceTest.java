/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.ANKIT;

import static junit.framework.TestCase.assertEquals;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.helpers.ext.azure.AzureParsedSecretReference;
import io.harness.rule.Owner;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class AzureParsedSecretReferenceTest extends CategoryTest {
  @Parameter(0) public String secretPath;

  @Parameter(1) public String secretName;

  @Parameter(2) public String secretVersion;

  @Parameter(3) public Class<? extends Exception> expectedException;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Parameters
  public static Iterable<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Test/", "Test", "", null},
        {"Secret/Version", "Secret", "Version", null},
        {"Secret", "Secret", "", null},
        {"/", "", "", null},
        {"/Ver", "", "Ver", null},
        {"Test//", "Test", "/", null},
        {"Test//Version/", "Test", "/Version/", null},
        {null, null, null, IllegalStateException.class},
        {"", null, null, IllegalStateException.class},
        {"  ", null, null, IllegalStateException.class},
        {"\t\t \t", null, null, IllegalStateException.class},
    });
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testAzureParsedSecretReference() {
    if (expectedException != null) {
      thrown.expect(expectedException);
    }

    AzureParsedSecretReference ref = new AzureParsedSecretReference(secretPath);
    assertEquals(secretName, ref.getSecretName());
    assertEquals(secretVersion, ref.getSecretVersion());
  }
}
