package io.piveau.translation;

import io.piveau.translation.database.DatabaseService;
import io.piveau.translation.database.DatabaseVerticle;
import io.piveau.translation.http.HttpServerVerticle;
import io.piveau.translation.request.TranslationRequestVerticle;
import io.piveau.translation.translation.TranslationService;
import io.piveau.translation.translation.TranslationVerticle;
import io.piveau.translation.util.ConfigConstant;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(VertxUnitRunner.class)
public class TranslationRequestTest {

  private static final Logger log = LoggerFactory.getLogger(TranslationRequestTest.class.getName());

  private Vertx vertx;
  private WebClient webClient;
  private DatabaseService translationDb;

  @Before
  public void prepare(TestContext context) {
    vertx = Vertx.vertx();

    // Config file reading
    ConfigStoreOptions envStoreOptions = new ConfigStoreOptions()
      .setType("env")
      .setConfig(new JsonObject().put("keys", new JsonArray()
        .add(ConfigConstant.TRANSLATION_SERVICE)
        .add(ConfigConstant.ETRANSLATION)
        .add(ConfigConstant.DATABASE)
      ));

    ConfigStoreOptions fileStoreOptions = new ConfigStoreOptions()
      .setType("file")
      .setConfig(new JsonObject().put("path", "conf/config.json"));

    ConfigRetriever retriever = ConfigRetriever
      .create(vertx, new ConfigRetrieverOptions()
        .addStore(fileStoreOptions)
        .addStore(envStoreOptions));

    // Deploy and start of nessessary verticles with given config file
    retriever.getConfig(ar -> {
      vertx.deployVerticle(DatabaseVerticle.class.getName(), new DeploymentOptions().setConfig(ar.result()), context.asyncAssertSuccess());
      vertx.deployVerticle(TranslationRequestVerticle.class.getName(), new DeploymentOptions().setConfig(ar.result()), context.asyncAssertSuccess());
      vertx.deployVerticle(HttpServerVerticle.class.getName(), new DeploymentOptions().setConfig(ar.result()), context.asyncAssertSuccess());
      vertx.deployVerticle(TranslationVerticle.class.getName(), new DeploymentOptions().setConfig(ar.result()), context.asyncAssertSuccess());
      webClient = WebClient.create(vertx, new WebClientOptions().setDefaultHost("localhost").setDefaultPort(8080));

      translationDb = DatabaseService.createProxy(vertx, DatabaseService.SERVICE_ADDRESS);
    });
  }

  @After
  public void finish(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void apiTest(TestContext context) {
    Async async = context.async();
    String tr_id = "test-dataset+test-catalog";

    // insert new receiver request
    Future<JsonObject> postRequest = Future.future();
    webClient.post("/receiver-service/")
      .as(BodyCodec.jsonObject())
      .sendJsonObject(getExampleTranslationRequest1(), ar -> {
        if (ar.succeeded()) {
          HttpResponse<JsonObject> postResponse = ar.result();
          postRequest.complete(postResponse.body());
        } else {
          context.fail(ar.cause());
        }
      });

    // update existing receiver request
    Future<JsonObject> updateRequest = Future.future();
    postRequest.compose(update -> {
      webClient.post("/receiver-service/")
        .as(BodyCodec.jsonObject())
        .sendJsonObject(getExampleTranslationRequest2(), ar -> {
          if (ar.succeeded()) {
            HttpResponse<JsonObject> updateResponse = ar.result();
            updateRequest.complete(updateResponse.body());
          } else {
            context.fail(ar.cause());
          }
//          async.complete();
        });
    }, updateRequest);

    Future<Void> deletionDbRequest = Future.future();
    updateRequest.compose(deletion -> {
      translationDb.deleteTranslationRequest(tr_id, dbResult -> {
        if (dbResult.succeeded()) {
          deletionDbRequest.succeeded();
        } else {
          log.error("Could not delete unit test data.");
          context.fail(dbResult.cause());
        }
        async.complete();
      });
    }, deletionDbRequest);

//    Future<JsonObject> getRequest = Future.future();
//    postRequest.compose(h -> {
//      webClient.get("/api/pages")
//        .as(BodyCodec.jsonObject())
//        .send(ar -> {
//          if (ar.succeeded()) {
//            HttpResponse<JsonObject> getResponse = ar.result();
//            getRequest.complete(getResponse.body());
//          } else {
//            context.fail(ar.cause());
//          }
//        });
//    }, getRequest);
//
//    Future<JsonObject> putRequest = Future.future();
//    getRequest.compose(response -> {
//      JsonArray array = response.getJsonArray("pages");
//      context.assertEquals(1, array.size());
//      context.assertEquals(0, array.getJsonObject(0).getInteger("id"));
//      webClient.put("/api/pages/0")
//        .as(BodyCodec.jsonObject())
//        .sendJsonObject(new JsonObject()
//          .put("id", 0)
//          .put("markdown", "Oh Yeah!"), ar -> {
//          if (ar.succeeded()) {
//            HttpResponse<JsonObject> putResponse = ar.result();
//            putRequest.complete(putResponse.body());
//          } else {
//            context.fail(ar.cause());
//          }
//        });
//    }, putRequest);
//
//    Future<JsonObject> deleteRequest = Future.future();
//    putRequest.compose(response -> {
//      context.assertTrue(response.getBoolean("success"));
//      webClient.delete("/api/pages/0")
//        .as(BodyCodec.jsonObject())
//        .send(ar -> {
//          if (ar.succeeded()) {
//            HttpResponse<JsonObject> delResponse = ar.result();
//            deleteRequest.complete(delResponse.body());
//          } else {
//            context.fail(ar.cause());
//          }
//        });
//    }, deleteRequest);
//
//    deleteRequest.compose(response -> {
//      context.assertTrue(response.getBoolean("success"));
//      async.complete();
//    }, Future.failedFuture("Oh?"));
  }

  private JsonObject getExampleTranslationRequest1() {
    return new JsonObject()
      .put("original_language", "en")
      .put("languages", new JsonArray().add("de").add("fi").add("pt").add("fr"))
      .put("callback", new JsonObject()
        .put("url", "http://locahost")
        .put("method", "POST")
        .put("payload", new JsonObject()
          .put("id", "test-dataset+test-catalog"))
        .put("headers", new JsonObject()
          .put("Authorization", "your-api-key")))
      .put("data_dict", new JsonObject()
        .put("title", "Example Dataset 2")
        .put("description", "This is an example dataset."));
  }

  private JsonObject getExampleTranslationRequest2() {
    return new JsonObject()
      .put("original_language", "de")
      .put("languages", new JsonArray().add("mt").add("ga").add("sv").add("nl"))
      .put("callback", new JsonObject()
        .put("url", "http://locahost")
        .put("method", "POST")
        .put("payload", new JsonObject()
          .put("id", "test-dataset+test-catalog"))
        .put("headers", new JsonObject()
          .put("Authorization", "your-api-key")))
      .put("data_dict", new JsonObject()
        .put("title", "Beispieldatensatz 2")
        .put("description", "Dies ist ein Beispiel Datensatz."));
  }
}
