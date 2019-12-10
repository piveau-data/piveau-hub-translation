package io.piveau.translation.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;

public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger log = LoggerFactory.getLogger(DatabaseServiceImpl.class);

  private final HashMap<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;

  public DatabaseServiceImpl(JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries, Handler<AsyncResult<DatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        log.error("Could not open a database connection", ar.cause());
        readyHandler.handle(Future.failedFuture(ar.cause()));
      } else {
        SQLConnection connection = ar.result();
        log.info("Database connection successful prepared.");

        // Creating all necessary relations in database
        Future<Void> createTranslationRequestFuture = Future.future();
        this.createTranslationRequest(createTranslationRequestFuture.completer());
        createTranslationRequestFuture
          .compose(dar2 -> {
            Future<Void> createTranslationFuture = Future.future();
            this.createTranslation(createTranslationFuture.completer());
            return createTranslationFuture;
          })
          .compose(dar3 -> {
            Future<Void> createAuthFuture = Future.future();
            this.createAuth(createAuthFuture.completer());
            return createAuthFuture;
          })
          .compose(dar4 -> {
            Future<Void> createFinishedTranslationFuture = Future.future();
            this.createFinishedTranslations(createFinishedTranslationFuture.completer());
            return createFinishedTranslationFuture;
          })
          .compose(dar5 -> {
            Future<Void> createActiveTranslationFuture = Future.future();
            this.createActiveTranslations(createActiveTranslationFuture.completer());
            return createActiveTranslationFuture;
          })
          .compose(dar6 -> {
            Future<Void> deleteInProgressActiveTranslationsFuture = Future.future();
            this.deleteAllActiveTranslations(deleteInProgressActiveTranslationsFuture.completer());
            return deleteInProgressActiveTranslationsFuture;
          })
        .compose(dar7 -> {
          Future<Void> deleteReceivedTranslationFuture = Future.future();
          this.deleteAllTranslations(deleteReceivedTranslationFuture.completer());
          return deleteReceivedTranslationFuture;
        })
        ;

        connection.close();
        readyHandler.handle(Future.succeededFuture(this));
      }
    });
  }

  @Override
  public DatabaseService createTranslationRequest(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.CREATE_TRANSLATION_REQUEST), dbResult -> {
      if (dbResult.succeeded()) {
        log.debug("Created relation translation_request successful.");
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService createTranslation(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.CREATE_TRANSLATION), dbResult -> {
      if (dbResult.succeeded()) {
        log.debug("Created relation translation successful.");
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService createAuth(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.CREATE_AUTH), dbResult -> {
      if (dbResult.succeeded()) {
        log.debug("Created relation auth successful.");
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService createFinishedTranslations(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.CREATE_FINISHED_TRANSLATIONS), dbResult -> {
      if (dbResult.succeeded()) {
        log.debug("Created relation finished_translation successful.");
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService createActiveTranslations(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.CREATE_ACTIVE_TRANSLATION), dbResult -> {
      if (dbResult.succeeded()) {
        log.debug("Created relation active_translation successful.");
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService insertTranslationRequest(String trId, String originalLanguage, String targetLanguages, int numTranslations, String dataDict, String callbackUrl, String callbackMethod, String callbackAuth, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId).add(originalLanguage).add(targetLanguages).add(numTranslations).add(dataDict).add(callbackUrl).add(callbackMethod).add(callbackAuth)
      .add(LocalDateTime.now().toString());
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.INSERT_TRANSLATION_REQUEST), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getNumTranslations(String trId, Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonArray param = new JsonArray().add(trId);
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_NUM_TRANSLATIONS), param, dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result().getNumRows() == 0) {
          resultHandler.handle(Future.succeededFuture(null));
        } else {
          resultHandler.handle(Future.succeededFuture(dbResult.result().getRows().get(0)));
        }
      } else {
        log.error("Database query error");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService updateTranslationRequest(String trId, String originalLanguage, String targetLanguage, int numTranslations, String dataDict, String callbackUrl, String callbackMethod, String callbackAuth, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(originalLanguage).add(targetLanguage).add(numTranslations).add(dataDict).add(callbackUrl).add(callbackMethod).add(callbackAuth)
      .add(LocalDateTime.now().toString()).add(trId);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.UPDATE_TRANSLATION_REQUEST), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService sendedTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.SENDED_TRANSLATION_REQUEST), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService unsendedTranslationRequests(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.update(sqlQueries.get(SqlQuery.UNSENDED_TRANSLATION_REQUESTS), dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService unsendOneTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(LocalDateTime.now().toString()).add(trId);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.UNSEND_ONE_TRANSLATION_REQUEST), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getTranslationRequest(String trId, Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_TRANSLATION_REQUEST), new JsonArray().add(trId), dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result().getNumRows() == 0) {
          log.debug("No entries for specifig trId found.");
          resultHandler.handle(Future.succeededFuture(null));
        } else {
          resultHandler.handle(Future.succeededFuture(dbResult.result().getRows().get(0)));
        }
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getOldestTranslationRequest(Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.GET_OLDEST_TRANSLATION_REQUEST), dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result().getNumRows() == 0) {
          // no waiting translation requests
          resultHandler.handle(Future.succeededFuture(null));
        } else {
          resultHandler.handle(Future.succeededFuture(dbResult.result().getRows().get(0)));
        }
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getAllTranslationRequests(Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.GET_ALL_TRANSLATION_REQUESTS), dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture(dbResult.result().toJson()));
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_TRANSLATION_REQUEST), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteCompletedTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_COMPLETED_TRANSLATION_REQUEST), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService insertTranslation(String trId, String language, String fieldName, String
    translatedText, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId).add(language).add(fieldName).add(translatedText);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.INSERT_TRANSLATION), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getTranslations(String trId, Handler<AsyncResult<JsonArray>> resultHandler) {
    JsonArray param = new JsonArray().add(trId);

    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_TRANSLATIONS), param, dbResult -> {
      if (dbResult.succeeded()) {
        JsonArray translations = new JsonArray(dbResult.result().getResults());
        resultHandler.handle(Future.succeededFuture(translations));
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getActualNumTranslations(String trId, Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_ACTUAL_NUM_TRANSLATIONS), new JsonArray().add(trId), dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result().getNumRows() == 0) {
          resultHandler.handle(Future.succeededFuture(null));
        } else {
          resultHandler.handle(Future.succeededFuture(dbResult.result().getRows().get(0)));
        }
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteTranslations(String trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_TRANSLATIONS), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteAllTranslations(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.DELETE_ALL_TRANSLATIONS), dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService insertAuth(String externalReference, String requestId, String
    trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(externalReference).add(requestId).add(trId).add(LocalDateTime.now().toString());
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.INSERT_AUTH), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getAuth(String externalReference, Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_AUTH), new JsonArray().add(externalReference), dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result().getNumRows() == 0) {
          resultHandler.handle(Future.succeededFuture(null));
        } else {
          resultHandler.handle(Future.succeededFuture(dbResult.result().getRows().get(0)));
        }
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteAuth(String externalReference, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(externalReference);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_AUTH), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService saveFinishedTranslation(String trId, String transmissionDate, String finishedDate,
                                                 int duration, String originalLanguage, int numTargetLanguages, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId).add(transmissionDate).add(finishedDate).add(duration).add(originalLanguage).add(numTargetLanguages);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.SAVE_FINISHED_TRANSLATION), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService insertActiveTranslation(String trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray params = new JsonArray().add(trId).add(LocalDateTime.now().toString());
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.INSERT_ACTIVE_TRANSLATION), params, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService findOldTranslation(Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.FIND_OLD_TRANSLATION_REQUEST), dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result().getNumRows() == 0) {
          resultHandler.handle(Future.succeededFuture(null));
        } else {
          resultHandler.handle(Future.succeededFuture(dbResult.result().getRows().get(0)));
        }
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteActiveTranslation(String trId, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray param = new JsonArray().add(trId);
    dbClient.updateWithParams(sqlQueries.get(SqlQuery.DELETE_ACTIVE_TRANSLATION), param, dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService deleteAllActiveTranslations(Handler<AsyncResult<Void>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.DELETE_ALL_ACTIVE_TRANSLATIONS), dbResult -> {
      if (dbResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService getNumActiveTranslations(Handler<AsyncResult<JsonObject>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.GET_NUM_ACTIVE_TRANSLATIONS), dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result().getNumRows() == 0) {
          resultHandler.handle(Future.succeededFuture(null));
        } else {
          resultHandler.handle(Future.succeededFuture(dbResult.result().getRows().get(0)));
        }
      } else {
        log.error("Database query error.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }
}
