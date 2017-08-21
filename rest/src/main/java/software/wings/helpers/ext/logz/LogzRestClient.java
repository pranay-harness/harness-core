package software.wings.helpers.ext.logz;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by rsingh on 8/21/17.
 */
public interface LogzRestClient { @POST("v1/search?size=10000") Call<Object> search(@Body Object logzFetchRequest); }
