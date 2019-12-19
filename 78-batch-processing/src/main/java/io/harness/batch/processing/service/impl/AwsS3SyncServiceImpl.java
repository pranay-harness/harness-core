package io.harness.batch.processing.service.impl;

import com.google.common.collect.Lists;

import io.harness.batch.processing.BatchProcessingException;
import io.harness.batch.processing.service.intfc.AwsS3SyncService;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Performs aws s3 sync.
 */
@Slf4j
public class AwsS3SyncServiceImpl implements AwsS3SyncService {
  private static final int SYNC_TIMEOUT_MINUTES = 5;

  /**
   * Syncs from src to dest.
   * Needs below permissions on src:
   * "s3:ListBucket"
   * "s3:GetObject"

   * Needs below permissions on dest:
   * "s3:PutObject"
   * "s3:PutObjectAcl"
   * </p>
   * @param src source bucket path.
   * @param srcRegion region of source bucket.
   * @param dest dest bucket path.
   */
  @Override
  public void syncBuckets(String src, String srcRegion, String dest) {
    try {
      final ArrayList<String> cmd = Lists.newArrayList("aws", "s3", "sync", src, dest, "--source-region", srcRegion);
      new ProcessExecutor()
          .command(cmd)
          .timeout(SYNC_TIMEOUT_MINUTES, TimeUnit.MINUTES)
          .redirectError(Slf4jStream.of(logger).asError())
          .exitValue(0)
          .execute();
    } catch (IOException | TimeoutException | InvalidExitValueException e) {
      logger.error("Exception during s3 sync for src={}, srcRegion={}, dest={}", src, srcRegion, dest);
      throw new BatchProcessingException("S3 sync failed", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
