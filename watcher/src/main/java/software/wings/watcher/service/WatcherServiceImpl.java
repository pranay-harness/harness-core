package software.wings.watcher.service;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.apache.commons.lang.StringUtils.substringBefore;

import com.google.inject.Singleton;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.watcher.app.WatcherConfiguration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by brett on 10/26/17
 */
@Singleton
public class WatcherServiceImpl implements WatcherService {
  private final Logger logger = LoggerFactory.getLogger(WatcherServiceImpl.class);
  private final Object waiter = new Object();
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject @Named("watchExecutor") private ScheduledExecutorService watchExecutor;
  @Inject private ExecutorService executorService;
  @Inject private UpgradeService upgradeService;
  @Inject private WatcherConfiguration watcherConfiguration;

  private String accountId;
  private AmazonS3Client amazonS3Client;

  @Override
  public void run(boolean upgrade) {
    try {
      accountId = watcherConfiguration.getAccountId();
      amazonS3Client = (AmazonS3Client) AmazonS3ClientBuilder.standard().withRegion("us-east-1").build();

      if (upgrade) {
        logger.info("[New] Upgraded watcher process started. Sending confirmation");
        System.out.println("watchstarted"); // Don't remove this. It is used as message in upgrade flow.
      } else {
        logger.info("Watcher process started");
      }

      if (watcherConfiguration.isDoUpgrade()) {
        startUpgradeCheck();
      }

      startWatching();

      if (upgrade) {
        logger.info("[New] Watcher upgraded");
      } else {
        logger.info("Watcher started");
      }

      synchronized (waiter) {
        waiter.wait();
      }

    } catch (Exception e) {
      logger.error("Exception while starting/running watcher", e);
    }
  }

  @Override
  public void stop() {
    synchronized (waiter) {
      waiter.notify();
    }
  }

  private void startUpgradeCheck() {
    upgradeExecutor.scheduleWithFixedDelay(
        ()
            -> {
          logger.info("Checking for upgrade");
          try {
            String watcherMetadataUrl = watcherConfiguration.getUpgradeCheckLocation();
            String bucketName =
                watcherMetadataUrl.substring(watcherMetadataUrl.indexOf("://") + 3, watcherMetadataUrl.indexOf(".s3"));
            String metaDataFileName = watcherMetadataUrl.substring(watcherMetadataUrl.lastIndexOf("/") + 1);
            S3Object obj = amazonS3Client.getObject(bucketName, metaDataFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(obj.getObjectContent()));
            String watcherMetadata = reader.readLine();
            String latestVersion = substringBefore(watcherMetadata, " ").trim();
            String watcherJarRelativePath = substringAfter(watcherMetadata, " ").trim();
            String version = getVersion();
            boolean upgrade = !StringUtils.equals(version, latestVersion);
            if (upgrade) {
              logger.info("[Old] Upgrading watcher");
              S3Object newVersionJarObj = amazonS3Client.getObject(bucketName, watcherJarRelativePath);

              upgradeService.doUpgrade(newVersionJarObj.getObjectContent(), getVersion(), latestVersion);
            } else {
              logger.info("Watcher up to date");
            }
          } catch (Exception e) {
            logger.error("[Old] Exception while checking for upgrade", e);
          }
        },
        watcherConfiguration.getUpgradeCheckIntervalSeconds(), watcherConfiguration.getUpgradeCheckIntervalSeconds(),
        TimeUnit.SECONDS);
  }

  private void startWatching() {
    watchExecutor.scheduleWithFixedDelay(
        ()
            -> {
                // TODO - watch delegate
            },
        1, 5, TimeUnit.SECONDS);
  }

  private String getVersion() {
    return System.getProperty("version", "1.0.0-DEV");
  }
}
