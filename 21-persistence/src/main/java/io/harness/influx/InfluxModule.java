package io.harness.influx;

import com.google.inject.AbstractModule;

import io.dropwizard.lifecycle.Managed;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Pong;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class InfluxModule extends AbstractModule implements Managed {
  private InfluxDB influxDB;
  private InfluxConfig influxConfig;
  private boolean dummy;
  private ThreadFactory threadFactory =
      ThreadPool.create(5, 20, 10, TimeUnit.MINUTES, Executors.defaultThreadFactory()).getThreadFactory();

  public InfluxModule(InfluxConfig influxConfig) {
    this.influxConfig = influxConfig;
  }

  @Override
  protected void configure() {
    try {
      if (StringUtils.isEmpty(influxConfig.getInfluxUri())) {
        logger.info("InfluxDB Configuration not found, will default to dummy configuration");
        dummy = true;
      } else {
        influxDB = checkInfluxDB(influxConfig);
        Pong pong = influxDB.ping();
        if (!pong.isGood()) {
          logger.error("InfluxDB connection does not look healthy");
          dummy = true;
        } else {
          logger.info("InfluxDB looks good, connected to [{}]", influxConfig.getInfluxUri());
        }
      }
    } catch (Exception e) {
      logger.error("InfluxDB connection does not look healthy", e);
      dummy = true;
    }
    if (dummy) {
      influxDB = getDummy();
    } else {
      influxDB = configureInflux(influxDB, influxConfig);
    }
    bind(InfluxDB.class).toInstance(influxDB);
  }

  private InfluxDB configureInflux(InfluxDB influxDB, InfluxConfig influxConfig) {
    return influxDB.setDatabase(influxConfig.getInfluxDatabase())
        .enableBatch(1000, 5, TimeUnit.SECONDS, threadFactory, (points, throwable) -> {
          logger.error("Error while trying to save dataPoints", throwable);
          points.forEach(point -> logger.error("HARNESS_TIMESERIES_FAILED_POINT :[{}]", point));
        });
  }

  private DummyInfluxDB getDummy() {
    return new DummyInfluxDB();
  }

  private InfluxDB checkInfluxDB(InfluxConfig config) {
    if (StringUtils.isEmpty(config.getInfluxUserName())) {
      return InfluxDBFactory.connect(config.getInfluxUri());
    } else {
      return InfluxDBFactory.connect(config.getInfluxUri(), config.getInfluxUserName(), config.getInfluxPassword());
    }
  }

  @Override
  public void start() throws Exception {}

  @Override
  public void stop() throws Exception {
    logger.info("Shutting down InfluxDB");
    influxDB.flush();
    influxDB.disableBatch();
  }
}
