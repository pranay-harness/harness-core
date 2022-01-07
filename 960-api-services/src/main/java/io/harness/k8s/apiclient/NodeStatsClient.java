/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s.apiclient;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.kubernetes.client.common.KubernetesType;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Pair;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Status;
import io.kubernetes.client.util.generic.KubernetesApiResponse;
import io.kubernetes.client.util.generic.options.ListOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.HttpUrl;

/**
 * The Generic kubernetes api provides a unified client interface for not only the non-core-group
 * built-in resources from kubernetes but also the custom-resources models meet the following
 * requirements:
 *
 * <p>1. there's a `V1ObjectMeta` field in the model along with its getter/setter. 2. there's a
 * `V1ListMeta` field in the list model along with its getter/setter. - supports Gson
 * serialization/deserialization. 3. the generic kubernetes api covers all the basic operations over
 * the custom resources including {get, list, watch, create, update, patch, delete}.
 *
 * <p>The Generic kubernetes api is expected to have the following behaviors upon failures where
 * {@link KubernetesApiResponse#isSuccess()} returns false:
 *
 * <p>- For kubernetes-defined failures, the server will return a {@link V1Status} with 4xx/5xx
 * code. The status object will be nested in {@link KubernetesApiResponse#getStatus()} - For the
 * other unknown reason (including network, JVM..), throws an unchecked exception.
 *
 * @param <ApiType> the api type parameter
 */

/*
 * This is a modified version of actual Generic Kubernetes api client to support node's kubelet's stats api.
 */
@OwnedBy(CE)
public class NodeStatsClient<ApiType extends KubernetesType> {
  // TODO(yue9944882): supports status operations..
  // TODO(yue9944882): supports generic sub-resource operations..
  // TODO(yue9944882): supports delete-collections..

  private Class<ApiType> apiTypeClass;
  private String apiGroup;
  private String apiVersion;
  private ApiClient localVarApiClient;
  private CustomObjectsApi customObjectsApi;

  /**
   * Instantiates a new Generic kubernetes api.
   *
   * @param apiTypeClass the api type class, e.g. V1Job.class
   * @param apiGroup the api group
   * @param apiVersion the api version
   * @param apiClient the api client
   */
  public NodeStatsClient(Class<ApiType> apiTypeClass, String apiGroup, String apiVersion, ApiClient apiClient) {
    this(apiTypeClass, apiGroup, apiVersion, new CustomObjectsApi(apiClient));
    this.localVarApiClient = apiClient;
  }

  /**
   * Instantiates a new Generic kubernetes api with the ApiClient specified.
   *
   * @param apiTypeClass the api type class, e.g. V1Job.class
   * @param apiGroup the api group
   * @param apiVersion the api version
   * @param customObjectsApi the custom objects api
   */
  public NodeStatsClient(
      Class<ApiType> apiTypeClass, String apiGroup, String apiVersion, CustomObjectsApi customObjectsApi) {
    this.apiGroup = apiGroup;
    this.apiVersion = apiVersion;
    this.apiTypeClass = apiTypeClass;
    this.customObjectsApi = customObjectsApi;
  }

  /**
   * List kubernetes api response cluster-scoped.
   *
   * @return the kubernetes api response
   */
  public KubernetesApiResponse<ApiType> list(String node) {
    return this.list(node, new ListOptions());
  }

  /**
   * List kubernetes api response.
   *
   * @param listOptions the list options
   * @return the kubernetes api response
   */
  public KubernetesApiResponse<ApiType> list(String node, final ListOptions listOptions) {
    return executeCall(this.localVarApiClient, apiTypeClass,
        ()
            -> this.listClusterCustomObjectCall(node, this.apiGroup, this.apiVersion, null, listOptions.getContinue(),
                listOptions.getFieldSelector(), listOptions.getLabelSelector(), listOptions.getLimit(),
                listOptions.getResourceVersion(), listOptions.getTimeoutSeconds(), Boolean.FALSE, null));
  }

  public Call listClusterCustomObjectCall(String node, String group, String version, String pretty, String _continue,
      String fieldSelector, String labelSelector, Integer limit, String resourceVersion, Integer timeoutSeconds,
      Boolean watch, ApiCallback _callback) throws ApiException {
    Object localVarPostBody = null;
    String localVarPath = "/api/{group}/{version}/nodes/{node}/proxy/stats/summary"
                              .replaceAll("\\{node\\}", this.localVarApiClient.escapeString(node))
                              .replaceAll("\\{group\\}", this.localVarApiClient.escapeString(group))
                              .replaceAll("\\{version\\}", this.localVarApiClient.escapeString(version));

    List<Pair> localVarQueryParams = new ArrayList();
    List<Pair> localVarCollectionQueryParams = new ArrayList();
    if (pretty != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("pretty", pretty));
    }

    if (_continue != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("continue", _continue));
    }

    if (fieldSelector != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("fieldSelector", fieldSelector));
    }

    if (labelSelector != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("labelSelector", labelSelector));
    }

    if (limit != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("limit", limit));
    }

    if (resourceVersion != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("resourceVersion", resourceVersion));
    }

    if (timeoutSeconds != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("timeoutSeconds", timeoutSeconds));
    }

    if (watch != null) {
      localVarQueryParams.addAll(this.localVarApiClient.parameterToPair("watch", watch));
    }

    Map<String, String> localVarHeaderParams = new HashMap();
    Map<String, String> localVarCookieParams = new HashMap();
    Map<String, Object> localVarFormParams = new HashMap();
    String[] localVarAccepts = new String[] {"application/json", "application/json;stream=watch"};
    String localVarAccept = this.localVarApiClient.selectHeaderAccept(localVarAccepts);
    if (localVarAccept != null) {
      localVarHeaderParams.put("Accept", localVarAccept);
    }

    String[] localVarContentTypes = new String[0];
    String localVarContentType = this.localVarApiClient.selectHeaderContentType(localVarContentTypes);
    localVarHeaderParams.put("Content-Type", localVarContentType);
    String[] localVarAuthNames = new String[] {"BearerToken"};
    return this.localVarApiClient.buildCall(localVarPath, "GET", localVarQueryParams, localVarCollectionQueryParams,
        localVarPostBody, localVarHeaderParams, localVarCookieParams, localVarFormParams, localVarAuthNames, _callback);
  }

  private static <DataType extends KubernetesType> KubernetesApiResponse<DataType> getKubernetesApiResponse(
      Class<DataType> dataClass, JsonElement element, Gson gson) {
    return getKubernetesApiResponse(dataClass, element, gson, 200);
  }

  private static <DataType extends KubernetesType> KubernetesApiResponse<DataType> getKubernetesApiResponse(
      Class<DataType> dataClass, JsonElement element, Gson gson, int httpStatusCode) {
    JsonElement kindElement = element.getAsJsonObject().get("kind");
    boolean isStatus = kindElement != null && "Status".equals(kindElement.getAsString());
    if (isStatus) {
      return new KubernetesApiResponse<>(gson.fromJson(element, V1Status.class), httpStatusCode);
    }
    return new KubernetesApiResponse<>(gson.fromJson(element, dataClass));
  }

  @SneakyThrows
  private <DataType extends KubernetesType> KubernetesApiResponse<DataType> executeCall(
      ApiClient apiClient, Class<DataType> dataClass, CallBuilder callBuilder) {
    try {
      Call call = callBuilder.build();
      call = tweakCallForCoreV1Group(call);
      JsonElement element = apiClient.<JsonElement>execute(call, JsonElement.class).getData();
      return getKubernetesApiResponse(dataClass, element, apiClient.getJSON().getGson());
    } catch (ApiException e) {
      if (e.getCause() instanceof IOException) {
        throw new IllegalStateException(e.getCause()); // make this a checked exception?
      }
      throw e;
      // we are not utilizing the v1Status response right now.
      /*
      final V1Status status;
      try {
        status = apiClient.getJSON().deserialize(e.getResponseBody(), V1Status.class);
      } catch (JsonSyntaxException jsonEx) {
        // make sure that the api server response is not lost while re-throwing the exception.
        throw new JsonSyntaxException(e.getResponseBody());
      }
      if (null == status) { // the response body can be something unexpected sometimes..
        throw new RuntimeException(e.getResponseBody());
      }
      return new KubernetesApiResponse<>(status, e.getCode());
       */
    }
  }

  // CallBuilder builds a call and throws ApiException otherwise.
  private interface CallBuilder {
    /**
     * Build call.
     *
     * @return the call
     * @throws ApiException the api exception
     */
    Call build() throws ApiException;
  }

  private Call tweakCallForCoreV1Group(Call call) {
    if (!apiGroup.equals("")) {
      return call;
    }
    HttpUrl url = call.request().url();
    HttpUrl tweakedUrl = url.newBuilder().removePathSegment(1).setPathSegment(0, "api").build();
    return this.customObjectsApi.getApiClient().getHttpClient().newCall(
        call.request().newBuilder().url(tweakedUrl).build());
  }
}
