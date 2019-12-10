package io.piveau.translation.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.HashMap;

@ProxyGen
@VertxGen
public interface DatabaseService {
  String SERVICE_ADDRESS = "io.piveau.translation.database.queue";

  @Fluent
  DatabaseService createTranslationRequest(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService createTranslation(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService createAuth(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService createFinishedTranslations(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService createActiveTranslations(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService insertTranslationRequest(String trId, String originalLanguage, String targetLanguages, int numTranslations, String dataDict, String callbackUrl, String callbackMethod, String callbackAuth, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService getNumTranslations(String trId, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService updateTranslationRequest(String trId, String originalLanguage, String targetLanguage, int numTranslations, String dataDict, String callbackUrl, String callbackMethod, String callbackAuth, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService sendedTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService unsendedTranslationRequests(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService unsendOneTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService getTranslationRequest(String irId, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService getOldestTranslationRequest(Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService getAllTranslationRequests(Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService deleteTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService deleteCompletedTranslationRequest(String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService insertTranslation(String trId, String language, String fieldName, String translatedText, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService getTranslations(String trId,  Handler<AsyncResult<JsonArray>> resultHandler);

  @Fluent
  DatabaseService getActualNumTranslations(String trId, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService deleteTranslations(String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService deleteAllTranslations(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService insertAuth(String externalReference, String requestId, String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService getAuth(String externalReference, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService deleteAuth(String externalReference, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService saveFinishedTranslation(String trId, String transmissionDate, String finishedDate, int duration, String originalLanguage, int numTargetLanguages, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService insertActiveTranslation(String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService findOldTranslation(Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DatabaseService deleteActiveTranslation(String trId, Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService deleteAllActiveTranslations(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  DatabaseService getNumActiveTranslations(Handler<AsyncResult<JsonObject>> resultHandler);

  @GenIgnore
  static DatabaseService create(JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries, Handler<AsyncResult<DatabaseService>> readyHandler) {
    return new DatabaseServiceImpl(dbClient, sqlQueries, readyHandler);
  }

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }
}
