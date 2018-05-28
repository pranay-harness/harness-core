package migrations.all;

import com.google.inject.Inject;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import migrations.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rsingh on 3/26/18.
 */
@SuppressFBWarnings("UUF_UNUSED_FIELD")
public class MetricDataMigration implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(TrimYamlMigration.class);

  @Inject WingsPersistence wingsPersistence;

  @SuppressFBWarnings("URF_UNREAD_FIELD") private double error = -1;

  // new relic metrics
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double throughput = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double averageResponseTime = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double apdexScore = -1;
  private double callCount;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double requestsPerMinute = -1;

  @SuppressFBWarnings("URF_UNREAD_FIELD") private double response95th = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double stalls = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double slowCalls = -1;

  @SuppressFBWarnings("URF_UNREAD_FIELD") private double clientSideFailureRate = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double errorCountHttp4xx = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double errorCountHttp5xx = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double requestsPerMin = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double responseTime = -1;
  @SuppressFBWarnings("URF_UNREAD_FIELD") private double serverSideFailureRate = -1;

  @Override
  public void migrate() {
    DBCursor newRelicMetricRecords = wingsPersistence.getCollection("newRelicMetricRecords").find();
    logger.info("will go through " + newRelicMetricRecords.size() + " records");

    int updated = 0;
    while (newRelicMetricRecords.hasNext()) {
      Map<String, Double> values = new HashMap<>();
      DBObject next = newRelicMetricRecords.next();

      String uuId = (String) next.get("_id");
      parseAndSetValue(next, "error", values);
      parseAndSetValue(next, "throughput", values);
      parseAndSetValue(next, "averageResponseTime", values);
      parseAndSetValue(next, "apdexScore", values);
      parseAndSetValue(next, "callCount", values);
      parseAndSetValue(next, "requestsPerMinute", values);

      parseAndSetValue(next, "response95th", values);
      parseAndSetValue(next, "stalls", values);
      parseAndSetValue(next, "slowCalls", values);

      parseAndSetValue(next, "clientSideFailureRate", values);
      parseAndSetValue(next, "errorCountHttp4xx", values);
      parseAndSetValue(next, "errorCountHttp5xx", values);
      parseAndSetValue(next, "requestsPerMin", values);
      parseAndSetValue(next, "responseTime", values);
      parseAndSetValue(next, "serverSideFailureRate", values);
      next.put("values", values);
      NewRelicMetricDataRecord metricDataRecord =
          JsonUtils.asObject(JsonUtils.asJson(next), NewRelicMetricDataRecord.class);
      metricDataRecord.setUuid(uuId);
      wingsPersistence.save(metricDataRecord);
      updated++;
    }

    logger.info("Complete. Updated " + updated + " records.");
  }

  private void parseAndSetValue(DBObject next, String key, Map<String, Double> values) {
    Object o = next.get(key);
    if (o == null) {
      return;
    }
    double value = o instanceof Long ? ((Long) o).doubleValue() : (double) o;

    next.removeField(key);
    if (value > -1) {
      values.put(key, value);
    }
  }
}
