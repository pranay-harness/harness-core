package io.harness.beans;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.jetty.util.LazyList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import javax.ws.rs.core.UriInfo;
/**
 * PageResponse bean class.
 *
 * @param <T> the generic type
 * @author Rishi
 */
@JsonFormat(shape = Shape.OBJECT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResponse<T> extends PageRequest<T> implements List<T> {
  private List<T> response = Lists.newArrayList();
  private Long total;

  /**
   * Instantiates a new page response.
   */
  public PageResponse() {}

  /**
   * Instantiates a new page response.
   *
   * @param req the req
   */
  public PageResponse(PageRequest<T> req) {
    super(req);
  }

  /**
   * Gets response.
   *
   * @return the response
   */
  public List<T> getResponse() {
    return response;
  }

  /**
   * Sets response.
   *
   * @param response the response
   */
  public void setResponse(List<T> response) {
    this.response = LazyList.isEmpty(response) ? Collections.emptyList() : response;
  }

  /**
   * Gets total.
   *
   * @return the total
   */
  public Long getTotal() {
    return total;
  }

  /**
   * Sets total.
   *
   * @param total the total
   */
  public void setTotal(Long total) {
    this.total = total;
  }

  /**
   * returns number of page for the collection.
   *
   * @return page number.
   */
  public long getCurrentPage() {
    if (getPageSize() == 0) {
      return 0;
    }
    int pageno = getStart() / getPageSize();
    if (getStart() % getPageSize() == 0) {
      pageno++;
    }
    return pageno;
  }

  /* (non-Javadoc)
   * @see java.util.List#size()
   */
  @Override
  public int size() {
    return response.size();
  }

  @Override
  public boolean isEmpty() {
    return response.isEmpty();
  }

  /* (non-Javadoc)
   * @see java.util.List#contains(java.lang.Object)
   */
  @Override
  public boolean contains(Object item) {
    return response.contains(item);
  }

  /* (non-Javadoc)
   * @see java.util.List#iterator()
   */
  @Override
  public Iterator<T> iterator() {
    return response.iterator();
  }

  /* (non-Javadoc)
   * @see java.util.List#toArray()
   */
  @Override
  public Object[] toArray() {
    return response.toArray();
  }

  /* (non-Javadoc)
   * @see java.util.List#toArray(java.lang.Object[])
   */
  @Override
  public <T1> T1[] toArray(T1[] array) {
    return response.toArray(array);
  }

  /* (non-Javadoc)
   * @see java.util.List#add(java.lang.Object)
   */
  @Override
  public boolean add(T item) {
    return response.add(item);
  }

  /* (non-Javadoc)
   * @see java.util.List#add(int, java.lang.Object)
   */
  @Override
  public void add(int index, T element) {
    response.add(index, element);
  }

  /* (non-Javadoc)
   * @see java.util.List#remove(java.lang.Object)
   */
  @Override
  public boolean remove(Object item) {
    return response.remove(item);
  }

  /* (non-Javadoc)
   * @see java.util.List#remove(int)
   */
  @Override
  public T remove(int index) {
    return response.remove(index);
  }

  /* (non-Javadoc)
   * @see java.util.List#containsAll(java.util.Collection)
   */
  @Override
  public boolean containsAll(Collection<?> collection) {
    return response.containsAll(collection);
  }

  /* (non-Javadoc)
   * @see java.util.List#addAll(java.util.Collection)
   */
  @Override
  public boolean addAll(Collection<? extends T> collection) {
    return response.addAll(collection);
  }

  /* (non-Javadoc)
   * @see java.util.List#addAll(int, java.util.Collection)
   */
  @Override
  public boolean addAll(int index, Collection<? extends T> collection) {
    return response.addAll(index, collection);
  }

  /* (non-Javadoc)
   * @see java.util.List#removeAll(java.util.Collection)
   */
  @Override
  public boolean removeAll(Collection<?> collection) {
    return response.removeAll(collection);
  }

  /* (non-Javadoc)
   * @see java.util.List#retainAll(java.util.Collection)
   */
  @Override
  public boolean retainAll(Collection<?> collection) {
    return response.retainAll(collection);
  }

  /* (non-Javadoc)
   * @see java.util.List#replaceAll(java.util.function.UnaryOperator)
   */
  @Override
  public void replaceAll(UnaryOperator<T> operator) {
    response.replaceAll(operator);
  }

  /* (non-Javadoc)
   * @see java.util.List#sort(java.util.Comparator)
   */
  @Override
  public void sort(Comparator<? super T> comparator) {
    response.sort(comparator);
  }

  /* (non-Javadoc)
   * @see java.util.List#clear()
   */
  @Override
  public void clear() {
    response.clear();
  }

  /* (non-Javadoc)
   * @see java.util.List#get(int)
   */
  @Override
  public T get(int index) {
    return response.get(index);
  }

  /* (non-Javadoc)
   * @see java.util.List#set(int, java.lang.Object)
   */
  @Override
  public T set(int index, T element) {
    return response.set(index, element);
  }

  /* (non-Javadoc)
   * @see java.util.List#indexOf(java.lang.Object)
   */
  @Override
  public int indexOf(Object item) {
    return response.indexOf(item);
  }

  /* (non-Javadoc)
   * @see java.util.List#lastIndexOf(java.lang.Object)
   */
  @Override
  public int lastIndexOf(Object item) {
    return response.lastIndexOf(item);
  }

  /* (non-Javadoc)
   * @see java.util.List#listIterator()
   */
  @Override
  public ListIterator<T> listIterator() {
    return response.listIterator();
  }

  /* (non-Javadoc)
   * @see java.util.List#listIterator(int)
   */
  @Override
  public ListIterator<T> listIterator(int index) {
    return response.listIterator(index);
  }

  /* (non-Javadoc)
   * @see java.util.List#subList(int, int)
   */
  @Override
  public List<T> subList(int fromIndex, int toIndex) {
    return response.subList(fromIndex, toIndex);
  }

  /* (non-Javadoc)
   * @see java.util.List#spliterator()
   */
  @Override
  public Spliterator<T> spliterator() {
    return response.spliterator();
  }

  /* (non-Javadoc)
   * @see java.util.Collection#removeIf(java.util.function.Predicate)
   */
  @Override
  public boolean removeIf(Predicate<? super T> filter) {
    return response.removeIf(filter);
  }

  /* (non-Javadoc)
   * @see java.util.Collection#stream()
   */
  @Override
  public Stream<T> stream() {
    return response.stream();
  }

  /* (non-Javadoc)
   * @see java.util.Collection#parallelStream()
   */
  @Override
  public Stream<T> parallelStream() {
    return response.parallelStream();
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#forEach(java.util.function.Consumer)
   */
  @Override
  public void forEach(Consumer<? super T> action) {
    response.forEach(action);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.PageRequest#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    PageResponse<?> that = (PageResponse<?>) obj;
    return total.equals(that.total) && Objects.equal(response, that.response);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.PageRequest#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(response, total);
  }

  /* (non-Javadoc)
   * @see software.wings.dl.PageRequest#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("response", response)
        .add("total", total)
        .add("currentPage", getCurrentPage())
        .toString();
  }

  /**
   * The type Builder.
   *
   * @param <T> the type parameter
   */
  public static final class PageResponseBuilder<T> {
    private List<T> response = Lists.newArrayList();
    private long total;
    private String offset;
    private String limit;
    private List<SearchFilter> filters = new ArrayList<>();
    private List<SortOrder> orders = new ArrayList<>();
    private List<String> fieldsIncluded = new ArrayList<>();
    private List<String> fieldsExcluded = new ArrayList<>();
    private UriInfo uriInfo;

    private PageResponseBuilder() {}

    /**
     * A page response builder.
     *
     * @return the builder
     */
    public static PageResponseBuilder aPageResponse() {
      return new PageResponseBuilder();
    }

    /**
     * With response builder.
     *
     * @param response the response
     * @return the builder
     */
    public PageResponseBuilder withResponse(List<T> response) {
      this.response = response;
      return this;
    }

    /**
     * With total builder.
     *
     * @param total the total
     * @return the builder
     */
    public PageResponseBuilder withTotal(long total) {
      this.total = total;
      return this;
    }

    /**
     * With offset builder.
     *
     * @param offset the offset
     * @return the builder
     */
    public PageResponseBuilder withOffset(String offset) {
      this.offset = offset;
      return this;
    }

    /**
     * With limit builder.
     *
     * @param limit the limit
     * @return the builder
     */
    public PageResponseBuilder withLimit(String limit) {
      this.limit = limit;
      return this;
    }

    /**
     * With filters builder.
     *
     * @param filters the filters
     * @return the builder
     */
    public PageResponseBuilder withFilters(List<SearchFilter> filters) {
      this.filters = filters;
      return this;
    }

    /**
     * With orders builder.
     *
     * @param orders the orders
     * @return the builder
     */
    public PageResponseBuilder withOrders(List<SortOrder> orders) {
      this.orders = orders;
      return this;
    }

    /**
     * With fields included builder.
     *
     * @param fieldsIncluded the fields included
     * @return the builder
     */
    public PageResponseBuilder withFieldsIncluded(List<String> fieldsIncluded) {
      this.fieldsIncluded = fieldsIncluded;
      return this;
    }

    /**
     * With fields excluded builder.
     *
     * @param fieldsExcluded the fields excluded
     * @return the builder
     */
    public PageResponseBuilder withFieldsExcluded(List<String> fieldsExcluded) {
      this.fieldsExcluded = fieldsExcluded;
      return this;
    }

    /**
     * With uri info builder.
     *
     * @param uriInfo the uri info
     * @return the builder
     */
    public PageResponseBuilder withUriInfo(UriInfo uriInfo) {
      this.uriInfo = uriInfo;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public PageResponseBuilder but() {
      return aPageResponse()
          .withResponse(response)
          .withTotal(total)
          .withOffset(offset)
          .withLimit(limit)
          .withFilters(filters)
          .withOrders(orders)
          .withFieldsIncluded(fieldsIncluded)
          .withFieldsExcluded(fieldsExcluded)
          .withUriInfo(uriInfo);
    }

    /**
     * Build page response.
     *
     * @return the page response
     */
    public PageResponse build() {
      PageResponse pageResponse = new PageResponse();
      pageResponse.setResponse(response);
      pageResponse.setTotal(total);
      pageResponse.setOffset(offset);
      pageResponse.setLimit(limit);
      pageResponse.setFilters(filters);
      pageResponse.setOrders(orders);
      pageResponse.setFieldsIncluded(fieldsIncluded);
      pageResponse.setFieldsExcluded(fieldsExcluded);
      pageResponse.setUriInfo(uriInfo);
      return pageResponse;
    }
  }
}
