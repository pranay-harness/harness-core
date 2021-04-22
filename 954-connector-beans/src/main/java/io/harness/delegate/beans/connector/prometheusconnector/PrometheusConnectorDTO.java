package io.harness.delegate.beans.connector.prometheusconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CV)
public class PrometheusConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {

    @NotNull String url;
    Set<String> delegateSelectors;

    public String getUrl() {
        return url.endsWith("/") ? url : url + "/";
    }

    @Override
    public List<DecryptableEntity> getDecryptableEntities() {
        return Collections.emptyList();
    }
}
