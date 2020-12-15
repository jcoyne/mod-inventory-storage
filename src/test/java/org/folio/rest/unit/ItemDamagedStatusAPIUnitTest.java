package org.folio.rest.unit;

import static org.folio.rest.impl.ItemDamagedStatusAPI.REFERENCE_TABLE;
import static org.folio.rest.support.db.ErrorFactory.getUUIDErrorMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response;

import io.vertx.pgclient.PgException;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.impl.ItemDamagedStatusAPI;
import org.folio.rest.jaxrs.model.ItemDamageStatus;
import org.folio.rest.jaxrs.model.ItemDamageStatuses;
import org.folio.rest.persist.PgExceptionUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.support.PostgresClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class ItemDamagedStatusAPIUnitTest {
  private static final String DEFAULT_QUERY = "";
  private static final int DEFAULT_OFFSET = 0;
  private static final int DEFAULT_LIMIT = 10;
  private static final String DEFAULT_LANGUAGE = "en";
  private static final HashMap<String, String> DEFAULT_HEADERS = new HashMap<>();
  private static final String INTERNAL_SERVER_ERROR_MSG = "Oops! Some error was happened on the server side.";
  @Rule
  public RunTestOnContext rule = new RunTestOnContext();
  @Mock
  private PostgresClientFactory pgClientFactory;
  @Mock
  private PostgresClient postgresClient;
  @InjectMocks
  private ItemDamagedStatusAPI itemDamagedStatusAPI = new ItemDamagedStatusAPI();


  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(pgClientFactory.getInstance(any(Context.class), any(Map.class))).thenReturn(postgresClient);
  }

  private Handler<AsyncResult<Response>> assertStatus(TestContext testContext, int expected) {
    return testContext.asyncAssertSuccess(response -> assertThat(response.getStatus(), is(expected)));
  }

  private class FailingItemDamagedStatusAPI extends ItemDamagedStatusAPI {
    @Override
    protected Future<ItemDamageStatuses> searchItemDamagedStatuses(
        String query,
        int offset,
        int limit,
        Map<String, String> okapiHeaders,
        Context vertxContext) throws FieldException {
      throw new RuntimeException("mock");
    }

    @Override
    protected Future<ItemDamageStatus> getItemDamagedStatus(
        String id,
        Map<String, String> okapiHeaders,
        Context vertxContext) {
      throw new RuntimeException("mock");
    }
  }

  @Test
  public void getItemDamagedStatusesShouldRespondWithBadRequestWhenAQueryIsBadFormatted(TestContext testContext) {
    RuntimeException exception = new RuntimeException(INTERNAL_SERVER_ERROR_MSG);
    doAnswer(setExceptionForHandlerArgument(6, exception))
      .when(postgresClient)
      .get(
        eq(REFERENCE_TABLE),
        eq(ItemDamageStatus.class),
        eq(new String[]{"*"}),
        any(CQLWrapper.class),
        eq(true),
        eq(true),
        any(Handler.class)
      );

    itemDamagedStatusAPI.getItemDamagedStatuses(
      "cql=bad*?/format",
      DEFAULT_OFFSET,
      DEFAULT_LIMIT,
      DEFAULT_LANGUAGE,
      DEFAULT_HEADERS,
      assertStatus(testContext, 400),
      rule.vertx().getOrCreateContext()
    );
  }

  @Test
  public void getItemDamagedStatusesShouldRespondWithServerErrorWhenUnexpectedExceptionIsThrown(TestContext testContext) {

    new FailingItemDamagedStatusAPI().getItemDamagedStatuses(
        DEFAULT_QUERY,
        DEFAULT_OFFSET,
        DEFAULT_LIMIT,
        DEFAULT_LANGUAGE,
        DEFAULT_HEADERS,
        assertStatus(testContext, 500),
        rule.vertx().getOrCreateContext()
      );
  }

  @Test
  public void postItemDamagedStatusesShouldReturnBadRequestWhenInputDataIsCorrupted(TestContext testContext) {
    PgException exception = PgExceptionUtil.createPgExceptionFromMap(getUUIDErrorMap());
    doAnswer(setExceptionForHandlerArgument(3, exception))
      .when(postgresClient)
      .save(
        eq(REFERENCE_TABLE),
        anyString(),
        any(ItemDamageStatus.class),
        any(Handler.class)
      );
    itemDamagedStatusAPI.postItemDamagedStatuses(
      DEFAULT_LANGUAGE,
      new ItemDamageStatus(),
      DEFAULT_HEADERS,
      assertStatus(testContext, 400),
      rule.vertx().getOrCreateContext()
    );
  }

  @Test
  public void postItemDamagedStatusesShouldReturnInternalServerErrorWhenUnexpectedExceptionIsThrown(TestContext testContext) {
    doThrow(new RuntimeException(INTERNAL_SERVER_ERROR_MSG))
      .when(postgresClient)
      .save(
        eq(REFERENCE_TABLE),
        anyString(),
        any(ItemDamageStatus.class),
        any(Handler.class)
      );
    itemDamagedStatusAPI.postItemDamagedStatuses(
      DEFAULT_LANGUAGE,
      new ItemDamageStatus(),
      DEFAULT_HEADERS,
      assertStatus(testContext, 500),
      rule.vertx().getOrCreateContext()
    );
  }

  @Test
  public void getItemDamagedStatusesByIdShouldReturnInternalServerErrorWhenUnexpectedExceptionIsThrown(TestContext testContext) {
    new FailingItemDamagedStatusAPI().getItemDamagedStatusesById(
      UUID.randomUUID().toString(),
      DEFAULT_LANGUAGE,
      DEFAULT_HEADERS,
      assertStatus(testContext, 500),
      rule.vertx().getOrCreateContext()
    );
  }

  @Test
  public void deleteItemDamagedStatusesByIdShouldReturnInternalServerErrorWhenUnexpectedExceptionIsThrown(TestContext testContext) {
    doThrow(new RuntimeException(INTERNAL_SERVER_ERROR_MSG))
      .when(postgresClient)
      .delete(
        eq(REFERENCE_TABLE),
        anyString(),
        any(Handler.class)
      );
    itemDamagedStatusAPI.deleteItemDamagedStatusesById(
      UUID.randomUUID().toString(),
      DEFAULT_LANGUAGE,
      DEFAULT_HEADERS,
      assertStatus(testContext, 500),
      rule.vertx().getOrCreateContext()
    );
  }

  @Test
  public void deleteItemDamagedStatusesByIdShouldReturnBadRequestWhenInputDataIsCorrupted(TestContext testContext) {
    PgException exception = PgExceptionUtil.createPgExceptionFromMap(getUUIDErrorMap());
    doAnswer(setExceptionForHandlerArgument(2, exception))
      .when(postgresClient)
      .delete(
        eq(REFERENCE_TABLE),
        anyString(),
        any(Handler.class)
      );
    itemDamagedStatusAPI.deleteItemDamagedStatusesById(
      UUID.randomUUID().toString(),
      DEFAULT_LANGUAGE,
      DEFAULT_HEADERS,
      assertStatus(testContext, 400),
      rule.vertx().getOrCreateContext()
    );
  }

  @Test
  public void putItemDamagedStatusesByIdShouldReturnBadRequestWhenInputDataIsCorrupted(TestContext testContext) {
    PgException exception = PgExceptionUtil.createPgExceptionFromMap(getUUIDErrorMap());
    doAnswer(setExceptionForHandlerArgument(3, exception))
      .when(postgresClient)
      .update(
        eq(REFERENCE_TABLE),
        any(ItemDamageStatus.class),
        anyString(),
        any(Handler.class)
      );
    itemDamagedStatusAPI.putItemDamagedStatusesById(
      UUID.randomUUID().toString(),
      DEFAULT_LANGUAGE,
      new ItemDamageStatus(),
      DEFAULT_HEADERS,
      assertStatus(testContext, 400),
      rule.vertx().getOrCreateContext()
    );
  }

  @Test
  public void putItemDamagedStatusesByIdShouldInternalServerErrorWhenUnexpectedExceptionIsThrown(TestContext testContext) {
    doThrow(new RuntimeException(INTERNAL_SERVER_ERROR_MSG))
      .when(postgresClient)
      .update(
        eq(REFERENCE_TABLE),
        any(ItemDamageStatus.class),
        anyString(),
        any(Handler.class)
      );
    itemDamagedStatusAPI.putItemDamagedStatusesById(
      UUID.randomUUID().toString(),
      DEFAULT_LANGUAGE,
      new ItemDamageStatus(),
      DEFAULT_HEADERS,
      assertStatus(testContext, 500),
      rule.vertx().getOrCreateContext()
    );
  }

  private Answer setExceptionForHandlerArgument(int indexOfHandler, Exception ex) {
    return invocation -> {
      Handler<AsyncResult<Results<ItemDamageStatus>>> handler = invocation.getArgument(indexOfHandler);
      handler.handle(Future.failedFuture(ex));
      return null;
    };
  }
}
