/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import io.harness.rule.EventRule;
import io.harness.rule.LifecycleRule;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;

@Slf4j
public abstract class EventTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public EventRule apiServiceRule = new EventRule(lifecycleRule.getClosingFactory());
}
