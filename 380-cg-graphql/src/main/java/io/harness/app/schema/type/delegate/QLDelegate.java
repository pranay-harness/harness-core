package software.wings.graphql.schema.type.delegate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.QLObject;


@Value
@Builder
@AllArgsConstructor
public class QLDelegate implements QLObject {
    String accountId;
    String delegateType;
    String delegateName;
    String hostName;
    String description;
    String ip;
    boolean pollingModeEnabled;
    String status;
    long lastHeartBeat;
    String version;
    String delegateProfileId;
}
