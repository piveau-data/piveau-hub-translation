package io.piveau.translation.translation;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface TranslationService {
  String SERVICE_ADDRESS = "io.piveau.translation.translation.queue";

  @Fluent
  TranslationService checkTranslationRequestStatus(Handler<AsyncResult<Void>> resultHandler);

  @Fluent
  TranslationService checkTranslationStatus(String trId, Handler<AsyncResult<Void>> resultHandler);

  @GenIgnore
  static TranslationService create(Vertx vertx, JsonObject config, Handler<AsyncResult<TranslationService>> readyHandler) {
    return new TranslationServiceImpl(vertx, config, readyHandler);
  }

  @GenIgnore
  static TranslationService createProxy(Vertx vertx, String address) {
    return new TranslationServiceVertxEBProxy(vertx, address);
  }
}
