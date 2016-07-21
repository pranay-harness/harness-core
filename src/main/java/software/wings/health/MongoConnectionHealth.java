package software.wings.health;

import com.google.common.collect.Lists;

import com.codahale.metrics.health.HealthCheck;
import com.mongodb.MongoClient;

import java.util.HashMap;
import java.util.Map;

/**
 * HealthCheck class for the MongoDB.
 *
 * @author Rishi
 */
public class MongoConnectionHealth extends HealthCheck {
  private final MongoClient mongo;

  /**
   * Instantiates a new mongo connection health.
   *
   * @param mongo the mongo
   */
  public MongoConnectionHealth(MongoClient mongo) {
    this.mongo = mongo;
    mongo.listDatabaseNames();
  }

  /* (non-Javadoc)
   * @see com.codahale.metrics.health.HealthCheck#check()
   */
  @Override
  protected Result check() throws Exception {
    Map<String, String> messageMap = new HashMap<>();
    try {
      messageMap.put("addresses", String.valueOf(mongo.getAllAddress()));
      messageMap.put("databaseNames", String.valueOf(Lists.newArrayList(mongo.listDatabaseNames())));
      return Result.healthy(messageMap.toString());
    } catch (Exception ex) {
      return Result.unhealthy("Cannot connect to " + mongo.getAllAddress());
    }
  }
}
