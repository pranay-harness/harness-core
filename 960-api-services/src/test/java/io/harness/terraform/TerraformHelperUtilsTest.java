/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.filesystem.FileIo;
import io.harness.rule.Owner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class TerraformHelperUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testgenerateCommandFlagsString() {
    List<String> listofArgs = Arrays.asList("arg1", "arg2");
    String result = TerraformHelperUtils.generateCommandFlagsString(listofArgs, "-command");
    assertThat(result).isNotNull();
    result.equals("-command=arg1 -command=arg2");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testgetTerraformStateFile() throws IOException {
    String scriptDirectory = "repository/testSaveAndGetTerraformStateFile";
    String workspace = "tfworkspace";
    FileIo.createDirectoryIfDoesNotExist(scriptDirectory);
    FileUtils.touch(new File(format("%s/terraform.tfstate.d/%s/terraform.tfstate", scriptDirectory, workspace)));
    File tfFile = TerraformHelperUtils.getTerraformStateFile(scriptDirectory, workspace);
    assertThat(tfFile).isNotNull();
    assertThat(tfFile.getName()).isEqualTo("terraform.tfstate");
    Files.deleteIfExists(Paths.get(tfFile.getPath()));
  }
}
