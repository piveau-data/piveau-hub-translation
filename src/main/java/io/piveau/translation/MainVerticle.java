package io.piveau.translation;

import io.piveau.translation.database.DatabaseVerticle;
import io.piveau.translation.http.HttpServerVerticle;
import io.piveau.translation.request.TranslationRequestVerticle;
import io.piveau.translation.receiver.TranslationReceiverVerticle;
import io.piveau.translation.translation.TranslationVerticle;
import io.piveau.translation.util.ConfigConstant;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Future<Void> startFuture) throws Exception {
    log.info("Starting Translation Service...");
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

    // Deploy of the verticles with specific config file
    retriever.getConfig(json -> {
      if (json.succeeded()) {
        log.info("Read configuration file successful.");
        JsonObject config = json.result();
        int instances = config.getJsonObject(ConfigConstant.ETRANSLATION).getInteger("simultanous_translations");

        Future<String> dbVerticleDeployment = Future.future();
        vertx.deployVerticle(DatabaseVerticle.class.getName(), new DeploymentOptions().setConfig(config).setInstances(instances), dbVerticleDeployment.completer());

        dbVerticleDeployment
          .compose(id1 -> {
            Future<String> translationRequestDeployment = Future.future();
            vertx.deployVerticle(TranslationRequestVerticle.class.getName(), new DeploymentOptions().setConfig(config).setInstances(1), translationRequestDeployment.completer());
            return translationRequestDeployment;
          })
          .compose(id2 -> {
            Future<String> translationReceiveDeployment = Future.future();
            vertx.deployVerticle(TranslationReceiverVerticle.class.getName(), new DeploymentOptions().setConfig(config).setInstances(1), translationReceiveDeployment.completer());
            return translationReceiveDeployment;
          })
          .compose(id3 -> {
            Future<String> httpVerticleDeployment = Future.future();
            vertx.deployVerticle(HttpServerVerticle.class.getName(), new DeploymentOptions().setConfig(config).setInstances(instances), httpVerticleDeployment.completer());
            return httpVerticleDeployment;
          })
          .compose(id4 -> {
            Future<String> translationDeployment = Future.future();
            vertx.deployVerticle(TranslationVerticle.class.getName(), new DeploymentOptions().setConfig(config).setInstances(1), translationDeployment.completer());
            return translationDeployment;
          })
          .setHandler(ar -> {
            if (ar.succeeded()) {
              startFuture.complete();
            } else {
              startFuture.fail(ar.cause());
            }
          });
      } else {
        log.error("Could not read configuration file.");
        startFuture.fail(json.cause());
      }
    });
  }
}
