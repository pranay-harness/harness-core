/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.jenkins;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.assistedinject.Assisted;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
@OwnedBy(CDC)
@TargetModule(_960_API_SERVICES)
public interface JenkinsFactory {
  /**
   * Creates a new Jenkins object.
   *
   * @param url the url
   * @return the jenkins
   */
  Jenkins createWithoutCredentials(@Assisted("url") String url);

  /**
   * Creates the.
   *
   * @param url       the url
   * @param username  the username
   * @param password  the password
   * @return the jenkins
   */
  Jenkins create(
      @Assisted("url") String url, @Assisted("username") String username, @Assisted("password") char[] password);

  /**
   *
   * @param url
   * @param token
   * @return
   */
  Jenkins create(@Assisted("url") String url, @Assisted("token") char[] token);
}
