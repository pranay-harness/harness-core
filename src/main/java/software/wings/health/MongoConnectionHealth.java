package software.wings.health;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.MongoClient;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 *  HealthCheck class for the MongoDB
 *
 *
 * @author Rishi
 *
 */
public class MongoConnectionHealth extends HealthCheck {
  private final MongoClient mongo;

  public MongoConnectionHealth(MongoClient mongo) {
    this.mongo = mongo;
    mongo.listDatabaseNames();
  }

  @Override
  protected Result check() throws Exception {
    Map<String, String> messageMap = new HashMap<>();
    try {
      messageMap.put("addresses", String.valueOf(mongo.getAllAddress()));
      messageMap.put("databaseNames", String.valueOf(Lists.newArrayList(mongo.listDatabaseNames())));
      return Result.healthy(messageMap.toString());
    } catch (Exception e) {
      return Result.unhealthy("Cannot connect to " + mongo.getAllAddress());
    }
  }
}
