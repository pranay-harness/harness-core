/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml;

import software.wings.integration.IntegrationTestBase;

import org.junit.Before;

/**
 * Created by bsollish on 8/10/17.
 */
public class YamlPayloadIntegrationTestBase extends IntegrationTestBase {
  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_NAME_POST = "TestAppPOST_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION_POST = "stuffPOST";
  private final String TEST_YAML_POST =
      "--- # app.yaml for new Application\nname: " + TEST_NAME_POST + "\ndescription: " + TEST_DESCRIPTION_POST;
  private final String TEST_NAME_PUT = "TestAppPUT_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION_PUT = "stuffPUT";
  private final String TEST_YAML_PUT =
      "--- # app.yaml for new Application\nname: " + TEST_NAME_PUT + "\ndescription: " + TEST_DESCRIPTION_PUT;

  @Override
  @Before
  public void setUp() throws Exception {
    loginAdminUser();
  }
}
