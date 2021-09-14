/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.example;

import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.Value;

@Value
public class SamplePerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String countryName;
}
