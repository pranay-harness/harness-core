/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.bazel;

import static io.harness.rule.OwnerRule.BRIJESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.resource.Project;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DEL)
public class DelegateBazelDependencyCheckTest extends CategoryTest {
  @Test
  @Owner(developers = BRIJESH)
  @Category({UnitTests.class})
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void testDependencyVersionsMatchInBazelWithRestCapsule() throws IOException {
    List<String> depsInMavenInstallJson = getDepsInMavenInstallJson();
    List<String> depsInRestCapsule = getDepsInRestCapsule();

    Set<String> mismatchedVersions = new HashSet<>();
    for (int i = 0; i < depsInMavenInstallJson.size(); i++) {
      String depInMavenInstallJson = depsInMavenInstallJson.get(i).replace("jar:", "");
      List<String> fieldsInMavenInstallJson = Arrays.asList(depsInMavenInstallJson.get(i).split(":"));
      for (int j = 0; j < depsInRestCapsule.size(); j++) {
        List<String> fieldsInRestCapsule = Arrays.asList(depsInRestCapsule.get(j).split(":"));
        if (fieldsInRestCapsule.size() < 3) {
          continue;
        }
        String depInRestCapsule = depsInRestCapsule.get(j).replace("jar:", "");
        int sizeOfMavenInstalDep = fieldsInMavenInstallJson.size();
        int sizeOfRestCapsuleDep = fieldsInRestCapsule.size();
        if (depsInMavenInstallJson.get(i).contains("jar")) {
          sizeOfMavenInstalDep--;
        }
        if (depsInRestCapsule.get(j).contains("jar")) {
          sizeOfRestCapsuleDep--;
        }
        if (fieldsInMavenInstallJson.get(0).equals(fieldsInRestCapsule.get(0))
            && fieldsInMavenInstallJson.get(1).equals(fieldsInRestCapsule.get(1))
            && sizeOfMavenInstalDep == sizeOfRestCapsuleDep) {
          if (!depInMavenInstallJson.equals(depInRestCapsule)) {
            mismatchedVersions.add(depInRestCapsule);
          }
          break;
        }
      }
    }

    assertThat(mismatchedVersions.stream().sorted()).hasSize(0);
  }

  List<String> getDepsInMavenInstallJson() throws IOException {
    String rootDirectory = Project.rootDirectory(DelegateBazelDependencyCheckTest.class);
    ObjectMapper mapper = new ObjectMapper();
    Map<String, HashMap<String, ?>> mavenInstallJson =
        mapper.readValue(Paths.get(rootDirectory + "/project/main_maven_install.json").toFile(), HashMap.class);
    List dependenciesInMavenInstallJson =
        (ArrayList<HashMap<String, ?>>) mavenInstallJson.get("dependency_tree").get("dependencies");
    Set<String> dependencySetInMavenInstallJson = new HashSet<>();
    for (int i = 0; i < dependenciesInMavenInstallJson.size(); i++) {
      dependencySetInMavenInstallJson.addAll(
          (ArrayList) ((HashMap) dependenciesInMavenInstallJson.get(i)).get("dependencies"));
      dependencySetInMavenInstallJson.add((String) ((HashMap) dependenciesInMavenInstallJson.get(i)).get("coord"));
      dependencySetInMavenInstallJson.addAll(
          (ArrayList) ((HashMap) dependenciesInMavenInstallJson.get(i)).get("directDependencies"));
    }
    List<String> depsInMavenInstallJson = new ArrayList<>();
    depsInMavenInstallJson.addAll(dependencySetInMavenInstallJson);
    Collections.sort(depsInMavenInstallJson);
    return depsInMavenInstallJson;
  }

  List<String> getDepsInRestCapsule() throws IOException {
    String absolutePath = Project.moduleDirectory(DelegateBazelDependencyCheckTest.class);
    Set<String> depSetInRestCapsule = new HashSet<>();
    BufferedReader bf =
        new BufferedReader(new FileReader(absolutePath + "/src/test/resources/dependenciesInRestCapsule.txt"));
    String line = bf.readLine();
    while (line != null) {
      depSetInRestCapsule.add(line.replace("\"", "").replace(",", ""));
      line = bf.readLine();
    }
    bf.close();
    List<String> depsInRestCapsule = new ArrayList<>();
    depsInRestCapsule.addAll(depSetInRestCapsule);
    Collections.sort(depsInRestCapsule);
    return depsInRestCapsule;
  }
}
