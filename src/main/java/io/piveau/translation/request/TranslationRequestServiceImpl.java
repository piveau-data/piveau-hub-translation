package io.piveau.translation.request;

import io.piveau.translation.database.DatabaseService;
import io.piveau.translation.translation.TranslationService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class TranslationRequestServiceImpl implements TranslationRequestService {

  private static final Logger log = LoggerFactory.getLogger(TranslationRequestServiceImpl.class);

  private final DatabaseService translationDb;
  private final TranslationService translationService;

  public TranslationRequestServiceImpl(Vertx vertx, Handler<AsyncResult<TranslationRequestService>> readyHandler) {
    translationDb = DatabaseService.createProxy(vertx, DatabaseService.SERVICE_ADDRESS);
    translationService = TranslationService.createProxy(vertx, TranslationService.SERVICE_ADDRESS);
    readyHandler.handle(Future.succeededFuture(this));
  }

  @Override
  public TranslationRequestService receiveTranslationRequest(JsonObject translationRequest, Handler<AsyncResult<Void>> resultHandler) {
    log.debug("Received new translation request.");
    log.debug(translationRequest.toString());

    // get all necessary informations
    String trId = translationRequest.getJsonObject("callback").getJsonObject("payload").getString("id");
    String originalLanguage = translationRequest.getString("original_language");
    String targetLanguages = translationRequest.getJsonArray("languages").toString();
    int numTranslations = translationRequest.getJsonArray("languages").size() * this.checkDataDict(translationRequest.getJsonObject("data_dict"));
    String dataDict = translationRequest.getJsonObject("data_dict").toString();
    String callbackUrl = translationRequest.getJsonObject("callback").getString("url");
    String callbackMethod = translationRequest.getJsonObject("callback").getString("method");
    String callbackAuth = translationRequest.getJsonObject("callback").getJsonObject("headers").getString("Authorization");

    // check if existing trId is already in database, if true, it will be an update not an insert
    translationDb.getTranslationRequest(trId, dbResult -> {
      if (dbResult.succeeded()) {
        if (dbResult.result() == null) {
          // insert new translation request cause it is not existing
          translationDb.insertTranslationRequest(trId, originalLanguage, targetLanguages, numTranslations, dataDict, callbackUrl, callbackMethod, callbackAuth, dbInsertResult -> {
            if (dbInsertResult.succeeded()) {
              log.debug("Translation request inserted in database successful.");
//              translationService.checkTranslationRequestStatus(ar -> {});
              resultHandler.handle(Future.succeededFuture());
            } else {
              log.error("Could not insert new translation request.");
              resultHandler.handle(Future.failedFuture(dbInsertResult.cause()));
            }
          });
        } else {
          // update existing translation request
          translationDb.updateTranslationRequest(trId, originalLanguage, targetLanguages, numTranslations, dataDict, callbackUrl, callbackMethod, callbackAuth, dbUpdateResult -> {
            if (dbUpdateResult.succeeded()) {
              log.debug("Translation request updated in database successful.");
//              translationService.checkTranslationRequestStatus(ar -> {});
              resultHandler.handle(Future.succeededFuture());
            } else {
              log.error("Could not update an existing translation request.");
              resultHandler.handle(Future.failedFuture(dbUpdateResult.cause()));
            }
          });
        }
        resultHandler.handle(Future.succeededFuture());
      } else {
        log.error("Could not check the database of existing entries.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
    return this;
  }

  private int checkDataDict(JsonObject dataDict) {
    int result = 0;
    Set<String> keys = dataDict.getMap().keySet();
    for (String key : keys) {
      if (!dataDict.getString(key).isEmpty()) {
        result++;
      }
    }
    return result;
  }
}
