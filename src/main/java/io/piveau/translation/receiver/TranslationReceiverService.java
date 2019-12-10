package io.piveau.translation.receiver;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@ProxyGen
@VertxGen
public interface TranslationReceiverService {
  String SERVICE_ADDRESS = "io.piveau.translation.receiver.queue";

  @Fluent
  TranslationReceiverService receiveTranslation(String externalReference, String targetLanguage, String translatedText, Handler<AsyncResult<Void>> resultHandler);

  @GenIgnore
  static TranslationReceiverService create(Vertx vertx, Handler<AsyncResult<TranslationReceiverService>> readyHandler) {
    return new TranslationReceiverServiceImpl(vertx, readyHandler);
  }

  @GenIgnore
  static TranslationReceiverService createProxy(Vertx vertx, String address) {
    return new TranslationReceiverServiceVertxEBProxy(vertx, address);
  }
}
