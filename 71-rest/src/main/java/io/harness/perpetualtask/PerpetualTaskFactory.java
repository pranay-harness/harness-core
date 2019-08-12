package io.harness.perpetualtask;

import com.google.protobuf.InvalidProtocolBufferException;

import io.harness.perpetualtask.example.SamplePerpetualTask;
import io.harness.perpetualtask.example.SampleTask.SamplePerpetualTaskParams;
import org.apache.commons.lang3.StringUtils;

public class PerpetualTaskFactory {
  public PerpetualTask newTask(PerpetualTaskId taskId, PerpetualTaskParams params)
      throws InvalidProtocolBufferException {
    String type = getClassName(params.getCustomizedParams().getTypeUrl());
    switch (type) {
      case "SamplePerpetualTaskParams":
        SamplePerpetualTaskParams sampleParams = params.getCustomizedParams().unpack(SamplePerpetualTaskParams.class);
        return new SamplePerpetualTask(taskId, sampleParams);
      default:
        break;
    }
    return null;
  }

  private String getClassName(String typeUrl) {
    String fullyQualifiedClassName = typeUrl.split("/")[1];
    return StringUtils.substringAfterLast(fullyQualifiedClassName, ".");
  }
}
