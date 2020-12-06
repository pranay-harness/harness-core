package io.harness.perpetualtask.example;

import io.harness.perpetualtask.PerpetualTaskClientParams;

import lombok.Value;

@Value
public class SamplePerpetualTaskClientParams implements PerpetualTaskClientParams {
  private String countryName;
}
