package io.piveau.translation.http;

import io.piveau.translation.receiver.TranslationReceiverService;
import io.piveau.translation.request.TranslationRequestService;
import io.piveau.translation.util.ConfigConstant;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


public class HttpServerVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(HttpServerVerticle.class);

  public static final String CONFIG_HTTP_SERVER_PORT = "port";

  private TranslationRequestService translationRequestService;
  private TranslationReceiverService translationReceiverService;

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    translationRequestService = TranslationRequestService.createProxy(vertx, TranslationRequestService.SERVICE_ADDRESS);
    translationReceiverService = TranslationReceiverService.createProxy(vertx, TranslationReceiverService.SERVICE_ADDRESS);

    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/et_success_handler").handler(this::receiveTranslationSuccessHandler);
    router.get("/et_error_handler").handler(this::receiveTranslationSuccessHandler);

    router.post().handler(BodyHandler.create());
    router.post("/translation-service").handler(this::receiveTranslationRequestHandler);
    router.post("/et_success_handler").handler(this::receiveTranslationSuccessHandler);
    router.post("/et_error_handler").handler(this::receiveTranslationFailureHandler);

    int portNumber = config().getJsonObject(ConfigConstant.TRANSLATION_SERVICE).getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
    server
      .requestHandler(router)
      .listen(portNumber, ar -> {
        if (ar.succeeded()) {
          log.info("HTTP Server running on port " + portNumber);
          startFuture.complete();
        } else {
          log.error("Could not start a HTTP Server", ar.cause());
          startFuture.fail(ar.cause());
        }
      });
  }

  private void indexHandler(RoutingContext context) {
    context.response().setStatusCode(200);
    context.response()
      .putHeader("Content-Type", "application/json")
      .putHeader("Access-Control-Allow-Origin", "*");

    JsonObject res = new JsonObject()
      .put("service_name", "piveau-receiver-service")
      .put("service_status", new JsonObject()
        .put("translation_service", "online")
        .put("database_service", "online")
        .put("piveau_api_service", "online")
        .put("etranslation_api_service", "online"));
    context.response().end(res.encode());
  }

  private void receiveTranslationRequestHandler(RoutingContext context) {
    JsonObject translationRequest = context.getBodyAsJson();
    if (!this.validateJsonDocument(context, translationRequest, "original_language", "languages", "callback", "data_dict")) {
      return;
    }
    log.debug("Receive translation request ...");
    translationRequestService.receiveTranslationRequest(translationRequest, serviceReply -> {
      JsonObject response = new JsonObject();
      if (serviceReply.succeeded()) {
        log.debug(response.toString());
        context.response().setStatusCode(200);
        context.response().putHeader("Content-Type", "application/json");
        response.put("success", true);
        response.put("message", "Received translation request successful.");
      } else {
        context.response().setStatusCode(500);
        context.response().putHeader("Content-Type", "application/json");
        response.put("success", false);
        response.put("message", "Translation request caused an error.");
      }
      context.response().end(response.encode());
    });
  }

  private void receiveTranslationSuccessHandler(RoutingContext context) {
    context.request().params().entries().forEach(param -> this.validateTranslationResponse(context, param.getKey()));
    log.debug("Receive translation: " + context.request().params().get("external-reference") + " :: " + context.request().params().get("target-language").toLowerCase());

    String externalReference = context.request().params().get("external-reference");
    String translatedText = context.request().params().get("translated-text");
    String targetLanguage = context.request().params().get("target-language").toLowerCase();

    this.translationReceiverService.receiveTranslation(externalReference, targetLanguage, translatedText, serviceReply -> {
      JsonObject respone = new JsonObject();
      if (serviceReply.succeeded()) {
        context.response().setStatusCode(200);
        context.response().putHeader("Content-Type", "application/json");
        respone.put("success", true);
        respone.put("message", "Received translation successful.");
      } else {
        log.debug("Internal Server Error by receiving translation.");
        context.response().setStatusCode(500);
        context.response().putHeader("Content-Type", "application/json");
        respone.put("success", false);
        respone.put("message", "Received translation caused an error.");
      }
      context.response().end(respone.encode());
    });

  }

  private void receiveTranslationFailureHandler(RoutingContext context) {
    log.debug("Received failure from etranslation.");
    context.response().setStatusCode(200);
    context.response().end();
  }

  private boolean validateTranslationResponse(RoutingContext context, String param) {
    JsonArray acceptedParams = new JsonArray().add("request-id").add("target-language").add("translated-text").add("external-reference");
    if (!acceptedParams.contains(param)) {
      // found not accepted parameter
      log.warn("Found unexpected param in response (from eTranslation): " + param);
      context.response().setStatusCode(400);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request")
        .encode());
      return false;
    }
    return true;
  }

  private boolean validateJsonDocument(RoutingContext context, JsonObject translationRequest, String... expectedKeys) {
    if (!Arrays.stream(expectedKeys).allMatch(translationRequest::containsKey)) {
      log.error("Bad receiver request: " + translationRequest.encodePrettily() + " from " + context.request().remoteAddress());
      context.response().setStatusCode(400);
      context.response().putHeader("Content-Type", "application/json");
      context.response().end(new JsonObject()
        .put("success", false)
        .put("error", "Bad request")
        .encode()
      );
      return false;
    }
    return true;
  }
}
