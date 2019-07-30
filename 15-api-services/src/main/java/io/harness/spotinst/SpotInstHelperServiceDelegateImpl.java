package io.harness.spotinst;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static io.harness.spotinst.model.SpotInstConstants.listElastiGroupsQueryTime;
import static io.harness.spotinst.model.SpotInstConstants.spotInstBaseUrl;
import static io.harness.spotinst.model.SpotInstConstants.spotInstContentType;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.stream.Collectors.toList;

import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ReportTarget;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupInstanceHealth;
import io.harness.spotinst.model.SpotInstListElastiGroupInstancesHealthResponse;
import io.harness.spotinst.model.SpotInstListElastiGroupsResponse;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

@Slf4j
public class SpotInstHelperServiceDelegateImpl implements SpotInstHelperServiceDelegate {
  private SpotInstRestClient getSpotInstRestClient() {
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(spotInstBaseUrl)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .client(getOkHttpClientBuilder().build())
                            .build();
    return retrofit.create(SpotInstRestClient.class);
  }

  private <T> T executeRestCall(Call<T> restRequest) throws Exception {
    Response<T> restResponse = restRequest.execute();
    if (!restResponse.isSuccessful()) {
      throw new WingsException(restResponse.errorBody().string(), EnumSet.of(ReportTarget.UNIVERSAL));
    }
    return restResponse.body();
  }

  private String getAuthToken(String spotInstToken) {
    return format("Bearer %s", spotInstToken);
  }

  @Override
  public List<ElastiGroup> listAllElastiGroups(
      String spotInstToken, String spotInstAccountId, String elastiGroupNamePrefix) throws Exception {
    String auth = getAuthToken(spotInstToken);
    String prefix = format("%s__", elastiGroupNamePrefix);
    long max = System.currentTimeMillis();
    long min = max - DAYS.toMillis(listElastiGroupsQueryTime);
    SpotInstListElastiGroupsResponse response = executeRestCall(
        getSpotInstRestClient().listAllElastiGroups(spotInstContentType, auth, min, max, spotInstAccountId));
    List<ElastiGroup> items = response.getResponse().getItems();
    if (isEmpty(items)) {
      return emptyList();
    }
    return items.stream()
        .filter(item -> {
          String name = item.getName();
          if (!name.startsWith(prefix)) {
            return false;
          }
          String temp = name.substring(prefix.length());
          return temp.matches("[0-9]+");
        })
        .sorted(Comparator.comparingInt(g -> Integer.parseInt(g.getName().substring(prefix.length()))))
        .collect(toList());
  }

  @Override
  public ElastiGroup createElastiGroup(String spotInstToken, String spotInstAccountId, String jsonPayload)
      throws Exception {
    String auth = getAuthToken(spotInstToken);
    SpotInstListElastiGroupsResponse spotInstListElastiGroupsResponse = executeRestCall(
        getSpotInstRestClient().createElastiGroup(spotInstContentType, auth, spotInstAccountId, jsonPayload));
    return spotInstListElastiGroupsResponse.getResponse().getItems().get(0);
  }

  @Override
  public void deleteElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception {
    String auth = getAuthToken(spotInstToken);
    executeRestCall(
        getSpotInstRestClient().deleteElastiGroup(spotInstContentType, auth, spotInstAccountId, elastiGroupId));
  }

  @Override
  public void scaleUpElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception {
    String auth = getAuthToken(spotInstToken);
    executeRestCall(getSpotInstRestClient().scaleUpElastiGroup(
        spotInstContentType, auth, spotInstAccountId, elastiGroupId, adjustment));
  }

  @Override
  public void scaleDownElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception {
    String auth = getAuthToken(spotInstToken);
    executeRestCall(getSpotInstRestClient().scaleDownElastiGroup(
        spotInstContentType, auth, spotInstAccountId, elastiGroupId, adjustment));
  }

  @Override
  public List<ElastiGroupInstanceHealth> listElastiGroupInstancesHealth(
      String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception {
    String auth = getAuthToken(spotInstToken);
    SpotInstListElastiGroupInstancesHealthResponse response =
        executeRestCall(getSpotInstRestClient().listElastiGroupInstancesHealth(
            spotInstContentType, auth, spotInstAccountId, elastiGroupId));
    return response.getResponse().getItems();
  }
}