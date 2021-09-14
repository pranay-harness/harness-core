/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.anomalydetection.pythonserviceendpoint;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.helpers.AnomalyDetectionHelper;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.ccm.anomaly.entities.Anomaly;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Service
@Slf4j
public class AnomalyDetectionPythonService {
  static int MAX_RETRY = 3;

  Retrofit retrofitClient;
  AnomalyDetectionEndPoint anomalyDetectionEndpoint;
  private BatchMainConfig mainConfig;

  @Autowired
  public AnomalyDetectionPythonService(BatchMainConfig config) {
    mainConfig = config;
    retrofitClient = new Retrofit.Builder()
                         .baseUrl(mainConfig.getCePythonServiceConfig().getPythonServiceUrl())
                         .addConverterFactory(GsonConverterFactory.create())
                         .build();
    anomalyDetectionEndpoint = retrofitClient.create(AnomalyDetectionEndPoint.class);
  }

  public Anomaly process(AnomalyDetectionTimeSeries timeSeries) {
    Anomaly resultAnomaly = null;

    List<AnomalyDetectionTimeSeries> listTimeSeries = Arrays.asList(timeSeries);
    List<PythonInput> pythonInputList =
        listTimeSeries.stream().map(PythonMappers::fromTimeSeries).collect(Collectors.toList());

    int count = 0;
    while (count < MAX_RETRY) {
      try {
        Response<List<PythonResponse>> response = anomalyDetectionEndpoint.prophet(pythonInputList).execute();
        if (response.isSuccessful()) {
          PythonResponse result = response.body().get(0);
          if (!result.getId().equals(timeSeries.getId())) {
            throw new AssertionError("result id and given id of time series didn't match");
          }
          resultAnomaly = PythonMappers.toAnomaly(result, timeSeries);
          log.info(
              "statistics : y_hat : [{}] , y_hat_lower : [{}] , y_hat_upper : [{}] ,  actual : [{}] , isAnomaly : [{}] ",
              result.getYHat(), result.getYHatLower(), result.getYHatUpper(), result.getY(), result.getIsAnomaly());
          break;
        } else {
          count++;
          AnomalyDetectionHelper.logUnsuccessfulHttpCall(response.code(), response.errorBody().string());
        }
      } catch (Exception e) {
        count++;
        log.error("could not make a successful http request to python service after count : {} , error {}", count, e);
      }
    }

    return resultAnomaly;
  }
}
