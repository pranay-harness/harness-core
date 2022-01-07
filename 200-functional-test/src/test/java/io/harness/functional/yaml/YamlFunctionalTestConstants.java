/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.yaml;

public interface YamlFunctionalTestConstants {
  String YAML_WEBHOOK_PAYLOAD_GITHUB = "{ \"ref\": \"refs/heads/master\"} ";

  String BASE_CLONE_PATH = "/tmp/test/clone";
  String BASE_GIT_REPO_PATH = "/tmp/test/gitRepo";
  String BASE_VERIFY_CLONE_PATH = "/tmp/test/verify";
}
