package io.harness;

import io.harness.eventsframework.Event;
import io.harness.eventsframework.ProjectUpdate;
import io.harness.eventsframework.RedisStreamClient;
import io.harness.lock.AcquiredLock;
import io.harness.lock.redis.RedisPersistentLocker;

import com.google.protobuf.InvalidProtocolBufferException;
import java.time.Duration;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageConsumer implements Runnable {
  String color;
  String readVia;
  RedisStreamClient client;
  String channel;
  String groupName;
  String consumerName;
  Integer processingTime;
  RedisPersistentLocker redisLocker;

  public MessageConsumer(String readVia, RedisStreamClient client, String channel, String color) {
    this.readVia = readVia;
    this.client = client;
    this.channel = channel;
    this.color = color;
  }

  public MessageConsumer(String readVia, RedisStreamClient client, String channel, String groupName,
      String consumerName, Integer processingTime, String color) {
    this(readVia, client, channel, color);
    this.groupName = groupName;
    this.consumerName = consumerName;
    this.processingTime = processingTime;
  }

  public MessageConsumer(RedisPersistentLocker redisLocker, String readVia, RedisStreamClient client, String channel,
      String groupName, String consumerName, Integer processingTime, String color) {
    this(readVia, client, channel, groupName, consumerName, processingTime, color);
    this.redisLocker = redisLocker;
  }

  @SneakyThrows
  @Override
  public void run() {
    if (this.readVia.equals("consumerGroups")) {
      readViaConsumerGroups();
    } else if (this.readVia.equals("serialConsumerGroups")) {
      readViaSerialConsumerGroups();
    } else {
      readViaPubSub();
    }
  }

  private void readViaConsumerGroups() throws InterruptedException {
    Map<String, Event> getValue;
    SortedSet<String> sortedKeys;
    while (true) {
      getValue = client.readEvent(channel, groupName, consumerName);
      sortedKeys = new TreeSet<String>(getValue.keySet());

      for (String key : sortedKeys) {
        ProjectUpdate p = null;
        String pid = "";
        try {
          p = getValue.get(key).getPayload().unpack(ProjectUpdate.class);
          pid = p.getProjectIdentifier();
          log.info("{}Received from redis as ConsumerGroup - {} - pid: {}{}", color, consumerName, pid,
              ColorConstants.TEXT_RESET);
        } catch (InvalidProtocolBufferException e) {
          log.error("{}Exception in unpacking data for key {}{}", color, key, ColorConstants.TEXT_RESET, e);
        }
        Thread.sleep(processingTime);
        log.info("{}Done processing for ConsumerGroup - {} - pid: {}{}", color, consumerName, pid,
            ColorConstants.TEXT_RESET);
        client.acknowledge(channel, groupName, key);
      }
    }
  }

  private void readViaSerialConsumerGroups() throws InterruptedException {
    Map<String, Event> getValue;
    SortedSet<String> sortedKeys;
    AcquiredLock lock;
    while (true) {
      lock = this.redisLocker.tryToAcquireLock(groupName, Duration.ofMinutes(1));
      if (lock == null) {
        Thread.sleep(1000);
        continue;
      }
      getValue = client.readEvent(channel, groupName, consumerName);
      sortedKeys = new TreeSet<String>(getValue.keySet());

      for (String key : sortedKeys) {
        ProjectUpdate p = null;
        String pid = "";
        try {
          p = getValue.get(key).getPayload().unpack(ProjectUpdate.class);
          pid = p.getProjectIdentifier();
          log.info("{}Received from redis as ConsumerGroup - {} - pid: {}{}", color, consumerName, pid,
              ColorConstants.TEXT_RESET);
        } catch (InvalidProtocolBufferException e) {
          log.error("{}Exception in unpacking data for key {}{}", color, key, ColorConstants.TEXT_RESET, e);
        }
        Thread.sleep(processingTime);
        log.info("{}Done processing for ConsumerGroup - {} - pid: {}{}", color, consumerName, pid,
            ColorConstants.TEXT_RESET);
        client.acknowledge(channel, groupName, key);
      }
      redisLocker.destroy(lock);
      Thread.sleep(200);
    }
  }

  private void readViaPubSub() throws InterruptedException {
    Map<String, Event> getValue;
    SortedSet<String> sortedKeys;
    String lastId = "$";
    while (true) {
      getValue = client.readEvent(channel, lastId);
      sortedKeys = new TreeSet<String>(getValue.keySet());
      for (String key : sortedKeys) {
        ProjectUpdate p = null;
        String pid = "";
        try {
          p = getValue.get(key).getPayload().unpack(ProjectUpdate.class);
          pid = p.getProjectIdentifier();
          log.info("{}Received from redis - pid: {}{}", color, pid, ColorConstants.TEXT_RESET);
        } catch (InvalidProtocolBufferException e) {
          log.error("{}Exception in unpacking data for key {}{}", color, key, ColorConstants.TEXT_RESET, e);
        }
        lastId = key;
      }
    }
  }
}
