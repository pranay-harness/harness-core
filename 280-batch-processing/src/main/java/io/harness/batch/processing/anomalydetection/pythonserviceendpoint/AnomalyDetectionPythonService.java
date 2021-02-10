package io.harness.batch.processing.anomalydetection.pythonserviceendpoint;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
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
          resultAnomaly = PythonMappers.toAnomaly(result, timeSeries);
          break;
        } else {
          log.error("unsuccessful http request from python server , error code {} ", response.code());
        }
      } catch (Exception e) {
        count++;
        log.error("could not make a successful http request to python service after count : {} , error {}", count, e);
      }
    }

    return resultAnomaly;
  }
}
