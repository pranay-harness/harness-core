package io.harness.pms.events;

public class PmsEventFrameworkConstants {
  public static final int MAX_TOPIC_SIZE = 1000000;

  public static final String INTERRUPT_PRODUCER = "INTERRUPT_PRODUCER";
  public static final String INTERRUPT_CONSUMER = "INTERRUPT_CONSUMER";
  public static final String INTERRUPT_TOPIC = "INTERRUPT_TOPIC";
  public static final String INTERRUPT_LISTENER = "INTERRUPT_LISTENER";
  public static final int INTERRUPT_BATCH_SIZE = 10;

  public static final String ORCHESTRATION_EVENT_PRODUCER = "ORCHESTRATION_EVENT_PRODUCER";
  public static final String ORCHESTRATION_EVENT_CONSUMER = "ORCHESTRATION_EVENT_CONSUMER";
  public static final String ORCHESTRATION_EVENT_TOPIC = "ORCHESTRATION_EVENT_TOPIC";
  public static final String ORCHESTRATION_EVENT_LISTENER = "ORCHESTRATION_EVENT_LISTENER";
  public static final int ORCHESTRATION_EVENT_BATCH_SIZE = 20;

  public static final String SERVICE_NAME = "SERVICE_NAME";

  public static final String SDK_RESPONSE_EVENT_PRODUCER = "SDK_RESPONSE_EVENT_PRODUCER";
  public static final String SDK_RESPONSE_EVENT_CONSUMER = "SDK_RESPONSE_EVENT_CONSUMER";
  public static final String SDK_RESPONSE_EVENT_TOPIC = "SDK_RESPONSE_EVENT_TOPIC";
  public static final String SDK_RESPONSE_EVENT_LISTENER = "SDK_RESPONSE_EVENT_LISTENER";
  public static final int SDK_RESPONSE_EVENT_BATCH_SIZE = 10;
}
