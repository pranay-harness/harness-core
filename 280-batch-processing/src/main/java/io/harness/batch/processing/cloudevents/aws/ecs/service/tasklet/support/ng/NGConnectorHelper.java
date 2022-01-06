/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.ng;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RestCallToNGManagerClientUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NGConnectorHelper {
  @Autowired private ConnectorResourceClient connectorResourceClient;

  public List<ConnectorResponseDTO> getNextGenConnectors(String accountId) {
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(ConnectorType.CE_AWS))
            .ccmConnectorFilter(
                CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.VISIBILITY)).build())
            .connectivityStatuses(Arrays.asList(ConnectivityStatus.SUCCESS))
            .build();
    connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
    int page = 0;
    int size = 100;
    do {
      response = getConnectors(accountId, page, size, connectorFilterPropertiesDTO);
      if (response != null && isNotEmpty(response.getContent())) {
        nextGenConnectorResponses.addAll(response.getContent());
      }
      page++;
    } while (response != null && isNotEmpty(response.getContent()));
    log.info("Processing batch size of {} in NG connector (From NG)", nextGenConnectorResponses.size());
    return nextGenConnectorResponses;
  }

  PageResponse getConnectors(
      String accountId, int page, int size, ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO) {
    return RestCallToNGManagerClientUtils.execute(
        connectorResourceClient.listConnectors(accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
  }
}
