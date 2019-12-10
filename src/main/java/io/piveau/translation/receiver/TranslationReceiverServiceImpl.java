package io.piveau.translation.receiver;

import io.piveau.translation.database.DatabaseService;
import io.piveau.translation.translation.TranslationService;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TranslationReceiverServiceImpl implements TranslationReceiverService {
  private static final Logger log = LoggerFactory.getLogger(TranslationReceiverServiceImpl.class);

  private DatabaseService translationDb;
  private TranslationService translationService;

  public TranslationReceiverServiceImpl(Vertx vertx, Handler<AsyncResult<TranslationReceiverService>> readyHandler) {
    this.translationDb = DatabaseService.createProxy(vertx, DatabaseService.SERVICE_ADDRESS);
    this.translationService = TranslationService.createProxy(vertx, TranslationService.SERVICE_ADDRESS);
    readyHandler.handle(Future.succeededFuture(this));
  }

  @Override
  public TranslationReceiverService receiveTranslation(String externalReference, String targetLanguage, String translatedText, Handler<AsyncResult<Void>> resultHandler) {
    String trId = this.parseTrId(externalReference);
    String textId = this.parseTextId(externalReference);

    // insert new translation
    this.translationDb.insertTranslation(trId, targetLanguage, textId, translatedText, dbResult -> {

      // check if translation complete
      if (dbResult.succeeded()) {
        this.translationService.checkTranslationStatus(trId, checkResult -> {
          if (checkResult.succeeded()) {
            log.debug("Translation complete for: " + trId);
          }
        });
      } else {
        log.error("Could not insert translation in database", dbResult.cause());
      }
    });

    resultHandler.handle(Future.succeededFuture());
    return this;
  }

  private String parseTrId(String externalReference) {
    return externalReference.split("[+]{3}")[0];
  }

  private String parseTextId(String externalReference) {
    return externalReference.split("[+]{3}")[1];
  }
}
