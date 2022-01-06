/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.rule;

import lombok.extern.slf4j.Slf4j;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

@Slf4j
public class OwnerWatcherRule extends TestWatcher {
  @Override
  protected void failed(Throwable e, Description description) {
    final Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return;
    }

    String jobBaseName = System.getenv("JOB_BASE_NAME");
    if ("portal".equals(jobBaseName) || "portal-functional-tests".equals(jobBaseName)) {
      OwnerRule.checkForJira(description.getDisplayName(), owner.developers()[0], OwnerRule.PRIORITY_VALUE0);
    }

    fileTest(owner, "failed");
  }

  @Override
  protected void skipped(AssumptionViolatedException e, Description description) {
    fileTest(description, "skipped");
  }

  private void fileTest(Description description, String type) {
    final Owner owner = description.getAnnotation(Owner.class);
    if (owner == null) {
      return;
    }

    fileTest(owner, type);
  }

  private void fileTest(Owner owner, String type) {
    for (String developer : owner.developers()) {
      OwnerRule.fileOwnerAs(developer, type);
    }
  }
}
