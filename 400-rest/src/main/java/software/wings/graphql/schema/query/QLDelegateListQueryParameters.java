package software.wings.graphql.schema.query;

import graphql.schema.DataFetchingFieldSelectionSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLDelegateListQueryParameters implements QLPageQueryParameters{
    String accountId;
    int limit;
    int offset;

    DataFetchingFieldSelectionSet selectionSet;

}
