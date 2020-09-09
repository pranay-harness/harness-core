package io.harness.task;

import com.google.inject.Inject;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.KryoSerializer;
import io.harness.task.converters.ResponseDataConverter;
import io.harness.task.converters.ResponseDataConverterRegistry;
import io.harness.task.service.HTTPTaskResponse;
import io.harness.task.service.JiraTaskResponse;
import io.harness.task.service.TaskType;
import io.harness.tasks.ResponseData;
import lombok.Builder;
import lombok.Data;

public class TaskServiceTestHelper {
  public static final String JIRA_ISSUE_KEY = "issueKey";
  public static final String JIRA_ISSUE_ID = "TEST-123";
  public static final String JIRA_DESCRIPTION = "description";
  public static final String HTTP_RESPONSE_BODY = "200 OK";
  public static final int HTTP_RESPONSE_CODE = 200;

  @Inject KryoSerializer kryoSerializer;

  public HTTPTaskResponse getHttpTaskResponse() {
    return HTTPTaskResponse.newBuilder()
        .setHttpResponseBody(HTTP_RESPONSE_BODY)
        .setHttpResponseCode(HTTP_RESPONSE_CODE)
        .build();
  }

  public JiraTaskResponse getJiraTaskResponse() {
    return JiraTaskResponse.newBuilder()
        .setDescription(JIRA_DESCRIPTION)
        .setId(JIRA_ISSUE_ID)
        .setKey(JIRA_ISSUE_KEY)
        .build();
  }

  public DummyHTTPResponseData getDummyHTTPResponseData() {
    return DummyHTTPResponseData.builder()
        .httpResponseBody(HTTP_RESPONSE_BODY)
        .httpResponseCode(HTTP_RESPONSE_CODE)
        .build();
  }

  public DummyJIRAResponseData getDummyJIRAResponseData() {
    return DummyJIRAResponseData.builder().id(JIRA_ISSUE_ID).key(JIRA_ISSUE_KEY).description(JIRA_DESCRIPTION).build();
  }

  public byte[] getDeflatedHttpResponseData() {
    return kryoSerializer.asDeflatedBytes(getDummyHTTPResponseData());
  }

  public byte[] getDeflatedJiraResponseData() {
    return kryoSerializer.asDeflatedBytes(getDummyJIRAResponseData());
  }

  @Data
  @Builder
  public static class DummyHTTPResponseData implements ResponseData {
    private int httpResponseCode;
    private String httpResponseBody;
  }
  @Data
  @Builder
  private static class DummyJIRAResponseData implements ResponseData {
    private String id;
    private String key;
    private String description;
  }

  @Builder
  public static class DummyHTTPResponseDataConverter implements ResponseDataConverter<HTTPTaskResponse> {
    @Override
    public HTTPTaskResponse convert(ResponseData responseData) {
      DummyHTTPResponseData dummyHTTPResponseData = (DummyHTTPResponseData) responseData;
      return HTTPTaskResponse.newBuilder()
          .setHttpResponseCode(dummyHTTPResponseData.getHttpResponseCode())
          .setHttpResponseBody(dummyHTTPResponseData.getHttpResponseBody())
          .build();
    }
  }

  @Builder
  public static class DummyJIRAResponseDataConverter implements ResponseDataConverter<JiraTaskResponse> {
    @Override
    public JiraTaskResponse convert(ResponseData responseData) {
      DummyJIRAResponseData dummyJIRAResponseData = (DummyJIRAResponseData) responseData;
      return JiraTaskResponse.newBuilder()
          .setKey(dummyJIRAResponseData.getKey())
          .setId(dummyJIRAResponseData.getId())
          .setDescription(dummyJIRAResponseData.getDescription())
          .build();
    }
  }

  public static void registerConverters(ResponseDataConverterRegistry registry) {
    registry.register(TaskType.HTTP, DummyHTTPResponseDataConverter.builder().build());
    registry.register(TaskType.JIRA, DummyJIRAResponseDataConverter.builder().build());
  }

  public static class TaskServiceTestKryoRegistrar implements KryoRegistrar {
    @Override
    public void register(Kryo kryo) {
      int index = 25 * 10000;
      kryo.register(DummyHTTPResponseData.class, ++index);
      kryo.register(DummyJIRAResponseData.class, ++index);
    }
  }

  public static Class<TaskServiceTestKryoRegistrar> getKryoRegistrar() {
    return TaskServiceTestKryoRegistrar.class;
  }
}
