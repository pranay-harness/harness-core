/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
