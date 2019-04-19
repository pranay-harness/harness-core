package software.wings.graphql.datafetcher.pipeline;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.graphql.datafetcher.AbstractConnectionDataFetcher;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;
import software.wings.graphql.schema.type.QLPipelineConnection;
import software.wings.graphql.schema.type.QLPipelineConnection.QLPipelineConnectionBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class PipelineConnectionDataFetcher extends AbstractConnectionDataFetcher<QLPipelineConnection> {
  protected static final String APP_ID_ARG = "appId";

  @Inject
  public PipelineConnectionDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  private static final PermissionAttribute permissionAttribute =
      new PermissionAttribute(PermissionType.PIPELINE, Action.READ);

  @Override
  public QLPipelineConnection fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    String appId = (String) getArgumentValue(dataFetchingEnvironment, APP_ID_ARG);

    Integer limit = (Integer) getArgumentValue(dataFetchingEnvironment, LIMIT_ARG);
    Integer offset = (Integer) getArgumentValue(dataFetchingEnvironment, OFFSET_ARG);
    if (offset == null) {
      offset = Integer.valueOf(0);
    }

    final Query<Pipeline> query = persistence.createQuery(Pipeline.class)
                                      .filter(PipelineKeys.appId, appId)
                                      .order(Sort.descending(PipelineKeys.createdAt));

    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder();

    FindOptions options = new FindOptions();

    pageInfoBuilder.limit(limit);
    options.limit(limit + 1);

    pageInfoBuilder.offset(offset);
    options.skip(offset);

    QLPipelineConnectionBuilder connectionBuilder = QLPipelineConnection.builder();
    try (HIterator<Pipeline> iterator = new HIterator<Pipeline>(query.fetch(options))) {
      for (int i = 0; i < limit && iterator.hasNext(); i++) {
        Pipeline pipeline = iterator.next();

        QLPipelineBuilder builder = QLPipeline.builder();
        PipelineController.populatePipeline(pipeline, builder);

        connectionBuilder.node(builder.build());
      }

      pageInfoBuilder.hasMore(iterator.hasNext());
    }

    if (isPageInfoTotalSelected(dataFetchingEnvironment)) {
      pageInfoBuilder.total((int) query.count());
    }

    return connectionBuilder.pageInfo(pageInfoBuilder.build()).build();
  }
}
