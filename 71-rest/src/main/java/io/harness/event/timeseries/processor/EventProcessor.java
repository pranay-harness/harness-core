package io.harness.event.timeseries.processor;

import io.harness.persistence.PersistentEntity;
import software.wings.service.impl.event.timeseries.TimeSeriesEventInfo;

public interface EventProcessor<T extends PersistentEntity> {
  /**
   EXECUTIONID TEXT NOT NULL,
   STARTTIME TIMESTAMP NOT NULL,
   ENDTIME TIMESTAMP NOT NULL,
   ACCOUNTID TEXT NOT NULL,
   APPID TEXT NOT NULL,
   TRIGGERED_BY TEXT,
   TRIGGER_ID TEXT,
   STATUS VARCHAR(20),
   SERVICES TEXT[],
   WORKFLOWS TEXT[],
   CLOUDPROVIDERS TEXT[],
   ENVIRONMENTS TEXT[],
   PIPELINE TEXT,
   DURATION BIGINT NOT NULL,
   ARTIFACTS TEXT[]
   */
  String EXECUTIONID = "EXECUTIONID";
  String STARTTIME = "STARTTIME";
  String ENDTIME = "ENDTIME";
  String ACCOUNTID = "ACCOUNTID";
  String APPID = "APPID";
  String TRIGGERED_BY = "TRIGGERED_BY";
  String TRIGGER_ID = "TRIGGER_ID";
  String STATUS = "STATUS";
  String SERVICE_LIST = "SERVICE_LIST";
  String WORKFLOW_LIST = "WORKFLOW_LIST";
  String CLOUD_PROVIDER_LIST = "CLOUD_PROVIDER_LIST";
  String ENV_LIST = "ENV_LIST";
  String PIPELINE = "PIPELINE";
  String DURATION = "DURATION";
  String ARTIFACT_LIST = "ARTIFACT_LIST";

  void processEvent(TimeSeriesEventInfo eventInfo);
}
