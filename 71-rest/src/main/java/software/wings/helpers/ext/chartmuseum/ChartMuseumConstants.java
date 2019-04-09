package software.wings.helpers.ext.chartmuseum;

public class ChartMuseumConstants {
  public static final int CHART_MUSEUM_SERVER_START_RETRIES = 5; // ToDo anshul decide on the value
  public static final int PORTS_START_POINT = 35000; // ToDo anshul decide on the value
  public static final int PORTS_BOUND = 5000; // ToDo anshul decide on the value

  public static final String CHART_MUSEUM_SERVER_URL = "http://localhost:${PORT}";

  public static final String NO_SUCH_BBUCKET_ERROR = "NoSuchBucket: The specified bucket does not exist";

  public static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
  public static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";

  public static final String S3_COMMAND_TEMPLATE =
      " --debug --port=${PORT} --storage=amazon --storage-amazon-bucket=${BUCKET_NAME} --storage-amazon-prefix=${FOLDER_PATH} --storage-amazon-region=${REGION}";
}
