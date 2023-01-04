/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.audit.api.streaming.impl;

import static io.harness.beans.SortOrder.Builder.aSortOrder;
import static io.harness.beans.SortOrder.OrderType.DESC;
import static io.harness.rule.OwnerRule.KAPIL;
import static io.harness.rule.OwnerRule.NISHANT;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.audit.entities.streaming.AwsS3StreamingDestination;
import io.harness.audit.entities.streaming.StreamingDestination;
import io.harness.audit.entities.streaming.StreamingDestination.StreamingDestinationKeys;
import io.harness.audit.entities.streaming.StreamingDestinationFilterProperties;
import io.harness.audit.mapper.streaming.StreamingDestinationMapper;
import io.harness.audit.repositories.streaming.StreamingDestinationRepository;
import io.harness.category.element.UnitTests;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NoResultFoundException;
import io.harness.ng.beans.PageRequest;
import io.harness.rule.Owner;
import io.harness.spec.server.audit.v1.model.AwsS3StreamingDestinationSpecDTO;
import io.harness.spec.server.audit.v1.model.StreamingDestinationDTO;
import io.harness.spec.server.audit.v1.model.StreamingDestinationDTO.StatusEnum;
import io.harness.spec.server.audit.v1.model.StreamingDestinationSpecDTO;
import io.harness.utils.PageUtils;

import com.mongodb.BasicDBList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RandomUtils;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public class StreamingServiceImplTest extends CategoryTest {
  private static final int RANDOM_STRING_CHAR_COUNT_10 = 10;
  private static final int RANDOM_STRING_CHAR_COUNT_15 = 15;
  private String accountIdentifier;
  private String id;
  private String slug;
  private String name;
  private StatusEnum statusEnum;
  private String bucket;
  private String connectorRef;

  @Mock private StreamingDestinationMapper streamingDestinationMapper;
  @Mock private StreamingDestinationRepository streamingDestinationRepository;
  private StreamingServiceImpl streamingService;

  @Rule public ExpectedException expectedException = ExpectedException.none();
  @Captor private ArgumentCaptor<StreamingDestination> streamingDestinationArgumentCaptor;
  @Captor private ArgumentCaptor<Criteria> criteriaArgumentCaptor;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.streamingService = new StreamingServiceImpl(streamingDestinationMapper, streamingDestinationRepository);

    accountIdentifier = randomAlphabetic(RANDOM_STRING_CHAR_COUNT_10);
    id = randomAlphabetic(RANDOM_STRING_CHAR_COUNT_10);
    slug = randomAlphabetic(RANDOM_STRING_CHAR_COUNT_10);
    name = randomAlphabetic(RANDOM_STRING_CHAR_COUNT_15);
    statusEnum = StatusEnum.values()[RandomUtils.nextInt(0, StatusEnum.values().length - 1)];
    bucket = randomAlphabetic(RANDOM_STRING_CHAR_COUNT_10);
    connectorRef = "account." + randomAlphabetic(RANDOM_STRING_CHAR_COUNT_10);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreate() {
    StreamingDestinationDTO streamingDestinationDTO = getStreamingDestinationDTO();
    StreamingDestination streamingDestination = getStreamingDestination();

    when(streamingDestinationMapper.toStreamingDestinationEntity(accountIdentifier, streamingDestinationDTO))
        .thenReturn(streamingDestination);
    when(streamingDestinationRepository.save(streamingDestination)).thenReturn(streamingDestination);

    StreamingDestination savedStreamingDestination =
        streamingService.create(accountIdentifier, streamingDestinationDTO);

    verify(streamingDestinationMapper, times(1))
        .toStreamingDestinationEntity(accountIdentifier, streamingDestinationDTO);
    verify(streamingDestinationRepository, times(1)).save(streamingDestinationArgumentCaptor.capture());

    assertThat(streamingDestinationArgumentCaptor.getValue()).isEqualTo(streamingDestination);
    assertThat(savedStreamingDestination).isNotNull();
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testCreateForDuplicateKeyException() {
    StreamingDestination streamingDestination = getStreamingDestination();
    StreamingDestinationDTO streamingDestinationDTO = getStreamingDestinationDTO();

    when(streamingDestinationMapper.toStreamingDestinationEntity(anyString(), any())).thenReturn(streamingDestination);
    when(streamingDestinationRepository.save(any())).thenThrow(new DuplicateKeyException("duplicate key error"));

    expectedException.expect(DuplicateFieldException.class);
    expectedException.expectMessage(
        String.format("Streaming destination with identifier [%s] already exists.", streamingDestinationDTO.getSlug()));

    streamingService.create(randomAlphabetic(RANDOM_STRING_CHAR_COUNT_10), streamingDestinationDTO);
  }

  @Test
  @Owner(developers = NISHANT)
  @Category(UnitTests.class)
  public void testList() {
    String searchTerm = randomAlphabetic(RANDOM_STRING_CHAR_COUNT_10);
    StreamingDestinationDTO.StatusEnum statusEnum = StreamingDestinationDTO.StatusEnum.ACTIVE;
    int page = 1;
    int limit = 10;
    Pageable pageable = PageUtils.getPageRequest(new PageRequest(
        page, limit, List.of(aSortOrder().withField(StreamingDestinationKeys.lastModifiedDate, DESC).build())));
    StreamingDestinationFilterProperties filterProperties =
        StreamingDestinationFilterProperties.builder().searchTerm(searchTerm).status(statusEnum).build();

    when(streamingDestinationRepository.findAll(any(), any()))
        .thenReturn(new PageImpl<StreamingDestination>(List.of(AwsS3StreamingDestination.builder().build())));

    Page<StreamingDestination> streamingDestinationsPage =
        streamingService.list(accountIdentifier, pageable, filterProperties);

    verify(streamingDestinationRepository, times(1)).findAll(criteriaArgumentCaptor.capture(), eq(pageable));

    assertThat(streamingDestinationsPage).isNotEmpty();
    assertCriteria(accountIdentifier, filterProperties, criteriaArgumentCaptor.getValue());
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testGetStreamingDestination() {
    StreamingDestination streamingDestination = getStreamingDestination();

    when(streamingDestinationRepository.findByAccountIdentifierAndIdentifier(anyString(), anyString()))
        .thenReturn(Optional.of(streamingDestination));

    StreamingDestination savedStreamingDestination = streamingService.getStreamingDestination(accountIdentifier, slug);

    verify(streamingDestinationRepository, times(1)).findByAccountIdentifierAndIdentifier(anyString(), anyString());

    assertThat(savedStreamingDestination).isEqualTo(streamingDestination);
    assertThat(savedStreamingDestination).isNotNull();
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testGetStreamingDestination_withNotFoundException() {
    when(streamingDestinationRepository.findByAccountIdentifierAndIdentifier(anyString(), anyString()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> streamingService.getStreamingDestination(accountIdentifier, slug))
        .hasMessage(String.format("Streaming destination with identifier [%s] not found.", slug))
        .isInstanceOf(NoResultFoundException.class);
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testDeleteStreamingDestination() {
    StreamingDestination streamingDestination = getStreamingDestination();
    streamingDestination.setStatus(StatusEnum.INACTIVE);

    when(streamingDestinationRepository.findByAccountIdentifierAndIdentifier(anyString(), anyString()))
        .thenReturn(Optional.of(streamingDestination));
    when(streamingDestinationRepository.deleteByCriteria(any())).thenReturn(Boolean.TRUE);

    boolean isDeleted = streamingService.delete(accountIdentifier, slug);

    verify(streamingDestinationRepository, times(1)).findByAccountIdentifierAndIdentifier(anyString(), anyString());
    verify(streamingDestinationRepository, times(1)).deleteByCriteria(criteriaArgumentCaptor.capture());

    assertThat(isDeleted).isTrue();
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testDeleteStreamingDestination_withInvalidRequestException() {
    StreamingDestination streamingDestination = getStreamingDestination();
    streamingDestination.setStatus(StatusEnum.ACTIVE);

    when(streamingDestinationRepository.findByAccountIdentifierAndIdentifier(anyString(), anyString()))
        .thenReturn(Optional.of(streamingDestination));
    when(streamingDestinationRepository.deleteByCriteria(any())).thenReturn(Boolean.TRUE);

    assertThatThrownBy(() -> streamingService.delete(accountIdentifier, slug))
        .hasMessage(
            String.format("Streaming destination with identifier [%s] cannot be deleted because it is active.", slug))
        .isInstanceOf(InvalidRequestException.class);

    verify(streamingDestinationRepository, times(1)).findByAccountIdentifierAndIdentifier(anyString(), anyString());
    verify(streamingDestinationRepository, times(0)).deleteByCriteria(criteriaArgumentCaptor.capture());
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testUpdateStreamingDestination() throws Exception {
    StreamingDestinationDTO streamingDestinationDTO = getStreamingDestinationDTO();
    streamingDestinationDTO.setName(name + " changed");
    streamingDestinationDTO.setStatus(StatusEnum.INACTIVE);
    streamingDestinationDTO.setSpec(new AwsS3StreamingDestinationSpecDTO()
                                        .bucket(bucket + " changed")
                                        .type(StreamingDestinationSpecDTO.TypeEnum.AWS_S3));

    StreamingDestination currentStreamingDestination = getStreamingDestination();

    AwsS3StreamingDestination newStreamingDestination = (AwsS3StreamingDestination) getStreamingDestination();
    newStreamingDestination.setName(name + " changed");
    newStreamingDestination.setStatus(StatusEnum.INACTIVE);
    newStreamingDestination.setBucket(bucket + " changed");

    when(streamingDestinationRepository.findByAccountIdentifierAndIdentifier(anyString(), anyString()))
        .thenReturn(Optional.of(currentStreamingDestination));
    when(streamingDestinationMapper.toStreamingDestinationEntity(anyString(), any()))
        .thenReturn(newStreamingDestination);
    when(streamingDestinationRepository.save(any())).thenReturn(newStreamingDestination);

    StreamingDestination responseStreamingDestination =
        streamingService.update(slug, streamingDestinationDTO, accountIdentifier);

    verify(streamingDestinationRepository, times(1)).findByAccountIdentifierAndIdentifier(anyString(), anyString());
    verify(streamingDestinationRepository, times(1)).save(any());

    assertThat(responseStreamingDestination).isEqualTo(newStreamingDestination);
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testUpdateStreamingDestination_withInvalidRequestException_forUnmatchedSlugInApiArgument() {
    StreamingDestinationDTO streamingDestinationDTO = getStreamingDestinationDTO();
    streamingDestinationDTO.setSlug(slug + " changed");

    StreamingDestination currentStreamingDestination = getStreamingDestination();
    when(streamingDestinationRepository.findByAccountIdentifierAndIdentifier(anyString(), anyString()))
        .thenReturn(Optional.of(currentStreamingDestination));

    assertThatThrownBy(() -> streamingService.update(slug, streamingDestinationDTO, accountIdentifier))
        .hasMessage(String.format(
            "Streaming destination with identifier [%s] did not match with StreamingDestinationDTO identifier [%s]",
            currentStreamingDestination.getIdentifier(), streamingDestinationDTO.getSlug()))
        .isInstanceOf(InvalidRequestException.class);

    verify(streamingDestinationRepository, times(1)).findByAccountIdentifierAndIdentifier(anyString(), anyString());
    verify(streamingDestinationRepository, times(0)).save(any());
  }

  @Test
  @Owner(developers = KAPIL)
  @Category(UnitTests.class)
  public void testUpdateStreamingDestination_withInvalidRequestException_forUnmatchedConnectorRef() {
    StreamingDestinationDTO streamingDestinationDTO = getStreamingDestinationDTO();
    streamingDestinationDTO.setConnectorRef(connectorRef + " changed");

    StreamingDestination currentStreamingDestination = getStreamingDestination();
    when(streamingDestinationRepository.findByAccountIdentifierAndIdentifier(anyString(), anyString()))
        .thenReturn(Optional.of(currentStreamingDestination));

    assertThatThrownBy(() -> streamingService.update(slug, streamingDestinationDTO, accountIdentifier))
        .hasMessage(String.format(
            "Streaming destination with connectorRef [%s] did not match with StreamingDestinationDTO connectorRef [%s]",
            currentStreamingDestination.getConnectorRef(), streamingDestinationDTO.getConnectorRef()))
        .isInstanceOf(InvalidRequestException.class);

    verify(streamingDestinationRepository, times(1)).findByAccountIdentifierAndIdentifier(anyString(), anyString());
    verify(streamingDestinationRepository, times(0)).save(any());
  }

  private void assertCriteria(
      String accountIdentifier, StreamingDestinationFilterProperties filterProperties, Criteria criteria) {
    assertThat(criteria.getCriteriaObject())
        .contains(Map.entry(StreamingDestinationKeys.accountIdentifier, accountIdentifier),
            Map.entry(StreamingDestinationKeys.status, filterProperties.getStatus()));
    assertThat(criteria.getCriteriaObject()).containsKey("$or");
    BasicDBList orCriteriaList = (BasicDBList) criteria.getCriteriaObject().get("$or");
    assertThat(orCriteriaList).isNotEmpty().hasSize(2);
    Document nameMatcher = (Document) orCriteriaList.get(0);
    assertThat(nameMatcher.get(StreamingDestinationKeys.name)).isInstanceOf(Pattern.class);
    Pattern nameMatcherPattern = (Pattern) nameMatcher.get(StreamingDestinationKeys.name);
    assertThat(nameMatcherPattern.pattern()).isEqualTo(filterProperties.getSearchTerm());
    assertThat(nameMatcherPattern.flags()).isEqualTo(Pattern.CASE_INSENSITIVE);
  }

  private StreamingDestinationDTO getStreamingDestinationDTO() {
    StreamingDestinationSpecDTO streamingDestinationSpecDTO =
        new AwsS3StreamingDestinationSpecDTO().bucket(bucket).type(StreamingDestinationSpecDTO.TypeEnum.AWS_S3);

    return new StreamingDestinationDTO()
        .slug(slug)
        .name(name)
        .status(statusEnum)
        .connectorRef(connectorRef)
        .spec(streamingDestinationSpecDTO);
  }

  private StreamingDestination getStreamingDestination() {
    StreamingDestination streamingDestination = AwsS3StreamingDestination.builder().bucket(bucket).build();
    streamingDestination.setId(id);
    streamingDestination.setIdentifier(slug);
    streamingDestination.setName(name);
    streamingDestination.setType(StreamingDestinationSpecDTO.TypeEnum.AWS_S3);
    streamingDestination.setStatus(statusEnum);
    streamingDestination.setAccountIdentifier(accountIdentifier);
    streamingDestination.setConnectorRef(connectorRef);

    return streamingDestination;
  }
}
