package io.harness.perpetualtask.example;

public interface SamplePTaskService {
  String create(String accountId, String countryName, int population);
  boolean update(String accountId, String taskId, String countryName, int population);
  int getPopulation(String countryName);
}
