package io.harness.spotinst;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static io.harness.spotinst.model.SpotInstConstants.listElastiGroupsQueryTime;
import static io.harness.spotinst.model.SpotInstConstants.spotInstBaseUrl;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.stream.Collectors.toList;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

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

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

  private List<ElastiGroup> listAllElstiGroups(String spotInstToken, String spotInstAccountId) throws Exception {
    String auth = getAuthToken(spotInstToken);
    long max = System.currentTimeMillis();
    long min = max - DAYS.toMillis(listElastiGroupsQueryTime);
    SpotInstListElastiGroupsResponse response =
        executeRestCall(getSpotInstRestClient().listAllElastiGroups(auth, min, max, spotInstAccountId));
    return response.getResponse().getItems();
  }

  private Map<String, Object> convertRawJsonToMap(String jsonToConvert) {
    Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(jsonToConvert, mapType);
  }

  @Override
  public Optional<ElastiGroup> getElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupName)
      throws Exception {
    List<ElastiGroup> items = listAllElstiGroups(spotInstToken, spotInstAccountId);
    if (isEmpty(items)) {
      return empty();
    }
    return items.stream().filter(group -> elastiGroupName.equals(group.getName())).findFirst();
  }

  @Override
  public List<ElastiGroup> listAllElastiGroups(
      String spotInstToken, String spotInstAccountId, String elastiGroupNamePrefix) throws Exception {
    List<ElastiGroup> items = listAllElstiGroups(spotInstToken, spotInstAccountId);
    if (isEmpty(items)) {
      return emptyList();
    }
    String prefix = format("%s__", elastiGroupNamePrefix);
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
  public void updateElastiGroup(
      String spotInstToken, String spotInstAccountId, String elastiGroupId, String jsonPayload) throws Exception {
    String auth = getAuthToken(spotInstToken);
    executeRestCall(getSpotInstRestClient().updateElastiGroup(
        auth, spotInstAccountId, elastiGroupId, convertRawJsonToMap(jsonPayload)));
  }

  @Override
  public ElastiGroup createElastiGroup(String spotInstToken, String spotInstAccountId, String jsonPayload)
      throws Exception {
    String auth = getAuthToken(spotInstToken);
    SpotInstListElastiGroupsResponse spotInstListElastiGroupsResponse = executeRestCall(
        getSpotInstRestClient().createElastiGroup(auth, spotInstAccountId, convertRawJsonToMap(jsonPayload)));
    return spotInstListElastiGroupsResponse.getResponse().getItems().get(0);
  }

  @Override
  public void deleteElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception {
    String auth = getAuthToken(spotInstToken);
    executeRestCall(getSpotInstRestClient().deleteElastiGroup(auth, spotInstAccountId, elastiGroupId));
  }

  @Override
  public void scaleUpElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception {
    String auth = getAuthToken(spotInstToken);
    executeRestCall(getSpotInstRestClient().scaleUpElastiGroup(auth, spotInstAccountId, elastiGroupId, adjustment));
  }

  @Override
  public void scaleDownElastiGroup(String spotInstToken, String spotInstAccountId, String elastiGroupId, int adjustment)
      throws Exception {
    String auth = getAuthToken(spotInstToken);
    executeRestCall(getSpotInstRestClient().scaleDownElastiGroup(auth, spotInstAccountId, elastiGroupId, adjustment));
  }

  @Override
  public List<ElastiGroupInstanceHealth> listElastiGroupInstancesHealth(
      String spotInstToken, String spotInstAccountId, String elastiGroupId) throws Exception {
    String auth = getAuthToken(spotInstToken);
    SpotInstListElastiGroupInstancesHealthResponse response =
        executeRestCall(getSpotInstRestClient().listElastiGroupInstancesHealth(auth, spotInstAccountId, elastiGroupId));
    return response.getResponse().getItems();
  }
}