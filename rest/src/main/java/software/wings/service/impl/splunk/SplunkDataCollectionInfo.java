package software.wings.service.impl.splunk;

import software.wings.beans.SplunkConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rsingh on 5/18/17.
 */
public class SplunkDataCollectionInfo {
  private SplunkConfig splunkConfig;
  private List<String> queries = new ArrayList<>();
  private int collectionTime;

  public SplunkDataCollectionInfo() {}

  public SplunkDataCollectionInfo(SplunkConfig splunkConfig, List<String> queries, int collectionTime) {
    this.splunkConfig = splunkConfig;
    this.queries = queries;
    this.collectionTime = collectionTime;
  }

  public SplunkConfig getSplunkConfig() {
    return splunkConfig;
  }

  public void setSplunkConfig(SplunkConfig splunkConfig) {
    this.splunkConfig = splunkConfig;
  }

  public List<String> getQueries() {
    return queries;
  }

  public void setQueries(List<String> queries) {
    this.queries = queries;
  }

  public int getCollectionTime() {
    return collectionTime;
  }

  public void setCollectionTime(int collectionTime) {
    this.collectionTime = collectionTime;
  }

  @Override
  public String toString() {
    return "SplunkDataCollectionInfo{"
        + "splunkConfig=" + splunkConfig + ", queries=" + queries + ", collectionTime=" + collectionTime + '}';
  }
}
