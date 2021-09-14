/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import io.harness.rules.VerificationTestRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Created by rsingh on 9/25/18.
 */
public abstract class VerificationBase extends CategoryTest {
  // I am not absolutely sure why, but there is dependency between wings io.harness.rule and
  // MockitoJUnit io.harness.rule and they have to be listed in these order
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public VerificationTestRule wingsRule = new VerificationTestRule();
}
