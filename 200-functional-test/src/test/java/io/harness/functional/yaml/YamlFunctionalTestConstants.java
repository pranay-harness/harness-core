/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.functional.yaml;

public interface YamlFunctionalTestConstants {
  String YAML_WEBHOOK_PAYLOAD_GITHUB = "{ \"ref\": \"refs/heads/master\"} ";

  String BASE_CLONE_PATH = "/tmp/test/clone";
  String BASE_GIT_REPO_PATH = "/tmp/test/gitRepo";
  String BASE_VERIFY_CLONE_PATH = "/tmp/test/verify";
}
