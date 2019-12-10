package io.piveau.translation.request;

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
public interface TranslationRequestService {
  String SERVICE_ADDRESS = "io.piveau.translation.request.queue";

  @Fluent
  TranslationRequestService receiveTranslationRequest(JsonObject translationRequest, Handler<AsyncResult<Void>> resultHandler);

  @GenIgnore
  static TranslationRequestService create(Vertx vertx, Handler<AsyncResult<TranslationRequestService>> readyHandler) {
    return new TranslationRequestServiceImpl(vertx, readyHandler);
  }

  @GenIgnore
  static TranslationRequestService createProxy(Vertx vertx, String address) {
    return new TranslationRequestServiceVertxEBProxy(vertx, address);
  }
}
