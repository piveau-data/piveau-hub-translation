package io.piveau.translation.database;

import io.piveau.translation.util.ConfigConstant;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

public class DatabaseVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(DatabaseVerticle.class);

  public static final String CONFIG_JDBC_URL = "jdbc_url";
  public static final String CONFIG_JDBC_DRIVER_CLASS = "jdbc_driver";
  public static final String CONFIG_JDBC_MAX_POOL_SIZE = "jdbc_max_pool_size";
  public static final String CONFIG_JDBC_SQL_QUERIES_RESOURCE_FILE = "sql_queries";
  public static final String CONFIG_JDBC_USER = "user";
  public static final String CONFIG_JDBC_PASSWORD = "password";



  private final HashMap<SqlQuery, String> sqlQueries = new HashMap<>();

  private JDBCClient dbClient;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    log.info("Start deployment of verticle " + DatabaseVerticle.class.getSimpleName());
    loadSqlQueries();

    dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", config().getJsonObject(ConfigConstant.DATABASE).getString(CONFIG_JDBC_URL, "jdbc:postgresql://localhost:5432/translation_db"))
      .put("driver_class", config().getJsonObject(ConfigConstant.DATABASE).getString(CONFIG_JDBC_DRIVER_CLASS, "org.postgresql.Driver"))
      .put("max_pool_size", config().getJsonObject(ConfigConstant.DATABASE).getInteger(CONFIG_JDBC_MAX_POOL_SIZE, 30))
      .put("user", config().getJsonObject(ConfigConstant.DATABASE).getString(CONFIG_JDBC_USER))
      .put("password", config().getJsonObject(ConfigConstant.DATABASE).getString(CONFIG_JDBC_PASSWORD))
    );

    DatabaseService.create(dbClient, sqlQueries, ready -> {
      if (ready.succeeded()) {
        ServiceBinder binder = new ServiceBinder(vertx);
        binder
          .setAddress(DatabaseService.SERVICE_ADDRESS)
          .register(DatabaseService.class, ready.result());
        log.info("Deployment of verticle " + DatabaseVerticle.class.getSimpleName() + " successful.");
        startFuture.complete();
      } else {
        log.error("Could not deploy verticle " + DatabaseVerticle.class.getSimpleName());
        startFuture.fail(ready.cause());
      }
    });
  }


  private void loadSqlQueries() throws IOException {
    String queriesFile = config().getJsonObject(ConfigConstant.DATABASE).getString(CONFIG_JDBC_SQL_QUERIES_RESOURCE_FILE, "doc/db-queries.properties");
    InputStream queriesInputStream;
    if (queriesFile != null) {
      queriesInputStream = new FileInputStream(queriesFile);
    } else {
      queriesInputStream = getClass().getResourceAsStream("/doc/db-queries.properties");
    }

    Properties queriesProps = new Properties();
    queriesProps.load(queriesInputStream);
    queriesInputStream.close();

    sqlQueries.put(SqlQuery.CREATE_TRANSLATION_REQUEST, queriesProps.getProperty("create-translation-request"));
    sqlQueries.put(SqlQuery.CREATE_TRANSLATION, queriesProps.getProperty("create-translation"));
    sqlQueries.put(SqlQuery.CREATE_AUTH, queriesProps.getProperty("create-auth"));
    sqlQueries.put(SqlQuery.CREATE_FINISHED_TRANSLATIONS, queriesProps.getProperty("create-finished-translations"));
    sqlQueries.put(SqlQuery.CREATE_ACTIVE_TRANSLATION, queriesProps.getProperty("create-active-translations"));
    sqlQueries.put(SqlQuery.INSERT_TRANSLATION_REQUEST, queriesProps.getProperty("insert-translation-request"));
    sqlQueries.put(SqlQuery.UPDATE_TRANSLATION_REQUEST, queriesProps.getProperty("update-translation-request"));
    sqlQueries.put(SqlQuery.SENDED_TRANSLATION_REQUEST, queriesProps.getProperty("sended-translation-request"));
    sqlQueries.put(SqlQuery.UNSENDED_TRANSLATION_REQUESTS, queriesProps.getProperty("unsended-translation-requests"));
    sqlQueries.put(SqlQuery.UNSEND_ONE_TRANSLATION_REQUEST, queriesProps.getProperty("unsend-one-translation-request"));
    sqlQueries.put(SqlQuery.GET_TRANSLATION_REQUEST, queriesProps.getProperty("get-translation-request"));
    sqlQueries.put(SqlQuery.GET_OLDEST_TRANSLATION_REQUEST, queriesProps.getProperty("get-oldest-translation-request"));
    sqlQueries.put(SqlQuery.GET_ALL_TRANSLATION_REQUESTS, queriesProps.getProperty("get-all-translation-requests"));
    sqlQueries.put(SqlQuery.GET_NUM_SENDED_TRANSLATION_REQUESTS, queriesProps.getProperty("get-num-sended-translation-requests"));
    sqlQueries.put(SqlQuery.DELETE_TRANSLATION_REQUEST, queriesProps.getProperty("delete-translation-request"));
    sqlQueries.put(SqlQuery.DELETE_COMPLETED_TRANSLATION_REQUEST, queriesProps.getProperty("delete-completed-translation-request"));
    sqlQueries.put(SqlQuery.INSERT_TRANSLATION, queriesProps.getProperty("insert-translation"));
    sqlQueries.put(SqlQuery.GET_TRANSLATIONS, queriesProps.getProperty("get-translations"));
    sqlQueries.put(SqlQuery.GET_NUM_TRANSLATIONS, queriesProps.getProperty("get-num-translations"));
    sqlQueries.put(SqlQuery.GET_ACTUAL_NUM_TRANSLATIONS, queriesProps.getProperty("get-actual-num-translations"));
    sqlQueries.put(SqlQuery.DELETE_TRANSLATIONS, queriesProps.getProperty("delete-translations"));
    sqlQueries.put(SqlQuery.DELETE_ALL_TRANSLATIONS, queriesProps.getProperty("delete-all-translations"));
    sqlQueries.put(SqlQuery.INSERT_AUTH, queriesProps.getProperty("insert-auth"));
    sqlQueries.put(SqlQuery.GET_AUTH, queriesProps.getProperty("get-auth"));
    sqlQueries.put(SqlQuery.DELETE_AUTH, queriesProps.getProperty("delete-auth"));
    sqlQueries.put(SqlQuery.SAVE_FINISHED_TRANSLATION, queriesProps.getProperty("save-finished-translation"));
    sqlQueries.put(SqlQuery.INSERT_ACTIVE_TRANSLATION, queriesProps.getProperty("insert-active-translation"));
    sqlQueries.put(SqlQuery.FIND_OLD_TRANSLATION_REQUEST, queriesProps.getProperty("find-old-translation"));
    sqlQueries.put(SqlQuery.DELETE_ACTIVE_TRANSLATION, queriesProps.getProperty("delete-active-translation"));
    sqlQueries.put(SqlQuery.DELETE_ALL_ACTIVE_TRANSLATIONS, queriesProps.getProperty("delete-all-active-translations"));
    sqlQueries.put(SqlQuery.GET_NUM_ACTIVE_TRANSLATIONS, queriesProps.getProperty("get-num-active-translations"));
  }
}
