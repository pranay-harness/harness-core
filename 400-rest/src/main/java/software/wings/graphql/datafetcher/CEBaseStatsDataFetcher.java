package software.wings.graphql.datafetcher;

public interface CEBaseStatsDataFetcher {
  /**
   * returning true will change the value of accountId to sampleAccountId (from config.yml) so that the data is
   * fetched for sampleAccountId.
   *
   * return false when in doubt.
   * @author UTSAV
   */
  boolean isCESampleAccountIdAllowed();
}
