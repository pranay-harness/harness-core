/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask.example;

public interface SamplePTaskService {
  String create(String accountId, String countryName, int population);
  boolean update(String accountId, String taskId, String countryName, int population);
  int getPopulation(String countryName);
}
