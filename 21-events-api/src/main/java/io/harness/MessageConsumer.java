package io.harness;

import io.harness.eventsframework.Event;
import io.harness.eventsframework.ProjectUpdate;
import io.harness.eventsframework.RedisStreamClient;
import io.harness.eventsframework.StreamChannel;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageConsumer implements Runnable {
  String readVia;
  RedisStreamClient client;
  StreamChannel channel;
  String groupName;
  String consumerName;

  public MessageConsumer(String readVia, RedisStreamClient client, StreamChannel channel) {
    this.readVia = readVia;
    this.client = client;
    this.channel = channel;
  }

  public MessageConsumer(
      String readVia, RedisStreamClient client, StreamChannel channel, String groupName, String consumerName) {
    this.readVia = readVia;
    this.client = client;
    this.channel = channel;
    this.groupName = groupName;
    this.consumerName = consumerName;
  }

  @SneakyThrows
  @Override
  public void run() {
    if (this.readVia == "consumerGroups") {
      readViaConsumerGroups();
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
        try {
          ProjectUpdate p = getValue.get(key).getPayload().unpack(ProjectUpdate.class);
          log.info("\u001B[36m"
              + "Received from redis as ConsumerGroup - " + consumerName + " - pid: " + p.getProjectId() + "\u001B[0m");
        } catch (InvalidProtocolBufferException e) {
          log.error("\u001B[36m"
                  + "Exception in unpacking data for key " + key + "\u001B[0m",
              e);
        }
        Thread.sleep(new Random().nextInt(2000));
        client.acknowledge(channel, groupName, key);
      }
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
        try {
          ProjectUpdate p = getValue.get(key).getPayload().unpack(ProjectUpdate.class);
          log.info("\u001B[32m"
              + "Received from redis as PubSub - pid: " + p.getProjectId() + "\u001B[0m");
        } catch (InvalidProtocolBufferException e) {
          log.error("\u001B[32m"
                  + "Exception in unpacking data for key " + key + "\u001B[0m",
              e);
        }
        lastId = key;
      }
    }
  }
}
