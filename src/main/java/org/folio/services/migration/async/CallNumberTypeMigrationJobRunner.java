package org.folio.services.migration.async;

import static java.lang.String.format;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.rest.jaxrs.model.AffectedEntity;
import org.folio.rest.persist.PostgresClientFuturized;
import org.folio.rest.persist.SQLConnection;
import org.folio.services.SystemCallNumberType;

public class CallNumberTypeMigrationJobRunner extends AbstractAsyncMigrationJobRunner {

  private static final String SELECT_ID_SQL = "SELECT id FROM %s WHERE %s";
  private static final String CONDITION_CN_TYPE_SQL = "(id!='%s' AND jsonb->>'name'='%s')";

  @Override
  public String getMigrationName() {
    return "callNumberTypeMigration";
  }

  @Override
  public List<AffectedEntity> getAffectedEntities() {
    return Collections.singletonList(AffectedEntity.ITEM);
  }

  @Override
  protected Future<RowStream<Row>> openStream(PostgresClientFuturized postgresClient, SQLConnection connection) {
    return postgresClient.selectStream(connection, selectOldTypeIds(postgresClient));
  }

  private String selectOldTypeIds(PostgresClientFuturized postgresClient) {
    String tableName = postgresClient.getFullTableName("call_number_type");
    String conditions = Arrays.stream(SystemCallNumberType.values())
      .map(type -> String.format(CONDITION_CN_TYPE_SQL, type.getId(), type.getName()))
      .collect(Collectors.joining(" OR "));

    return String.format(SELECT_ID_SQL, tableName, conditions);
  }
}
