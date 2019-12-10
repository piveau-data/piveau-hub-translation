package io.piveau.translation.translation;

import io.piveau.translation.database.DatabaseService;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;


public class TranslationServiceImpl implements TranslationService {

  private static final Logger log = LoggerFactory.getLogger(TranslationServiceImpl.class);

  public static final String CONFIG_ETRANSLATION_USER = "user";
  public static final String CONFIG_ETRANSLATION_APP = "application";
  public static final String CONFIG_ETRANSLATION_PASSWORD = "password";
  public static final String CONFIG_ETRANSLATION_URL = "e_translation_url";
  public static final String CONFIG_ETRANSLATION_CALLBACK = "callback_url";
  public static final String CONFIG_ETRANSLATION_SIMULTANOUS_TRANSLATIONS = "simultanous_translations";
  private static final int TRANSLATION_WAITING_TIME = 10; // in minutes
  private static final int PERIODIC_CHECK = 15000; // in milliseconds
  private static HashSet<String> completeTranslations;

  private final int simultanousTranslations;
  private DatabaseService translationDb;
  private JsonObject config;
  private Vertx vertx;

  public TranslationServiceImpl(Vertx vertx, JsonObject config, Handler<AsyncResult<TranslationService>> readyHandler) {
    this.translationDb = DatabaseService.createProxy(vertx, DatabaseService.SERVICE_ADDRESS);
    this.config = config;
    this.simultanousTranslations = this.config.getInteger(CONFIG_ETRANSLATION_SIMULTANOUS_TRANSLATIONS);
    this.vertx = vertx;
    completeTranslations = new HashSet<String>();
    readyHandler.handle(Future.succeededFuture(this));

    // Initial translation process
    this.translationDb.unsendedTranslationRequests(ar -> {});
    this.mainLoop();
  }

  private void mainLoop() {
    this.vertx.setPeriodic(PERIODIC_CHECK, handler -> {
      this.checkTranslationRequestStatus(ar -> {});
      this.identifyOldTranslations();
    });
  }

  private void identifyOldTranslations() {
    translationDb.findOldTranslation(dbResult -> {
      if (dbResult.result() != null) {
        String trId = dbResult.result().getString("tr_id");
        LocalDateTime sendedDate = LocalDateTime.parse(dbResult.result().getString("sended_date"));
        sendedDate = LocalDateTime.from(sendedDate);
        if (sendedDate.until(LocalDateTime.now(), ChronoUnit.MINUTES) >= TRANSLATION_WAITING_TIME) {
          // found old translation
          translationDb.unsendOneTranslationRequest(trId, ar -> {
            if (ar.succeeded()) {
              log.debug("Found one outdated translation request with id " + trId);
              translationDb.deleteActiveTranslation(trId, deleteResult -> {
                if (deleteResult.failed()) {
                  log.error("Could not delete active translation marked as old translation.");
                }
              });
              translationDb.deleteTranslations(trId, deleteResult -> {
                if (deleteResult.failed()) {
                  log.error("Could not delete translations.");
                }
              });
            }
          });
        }
      }
    });
  }

  @Override
  public TranslationService checkTranslationRequestStatus(Handler<AsyncResult<Void>> resultHandler) {
    translationDb.getNumActiveTranslations(activeTranslationResult -> {
      if (activeTranslationResult.succeeded() && activeTranslationResult.result().getInteger("count") < simultanousTranslations) {
        log.debug("Active translations: " + activeTranslationResult.result().getInteger("count") + " of max " + simultanousTranslations + " allowed.");

        // free slots for more translations are available
        translationDb.getOldestTranslationRequest(dbResult -> {
          if (dbResult.succeeded()) {
            JsonObject translationRequest = dbResult.result();
            if (translationRequest == null) {
              // no translation request in queue
              log.debug("No waiting translation request available");
              resultHandler.handle(Future.succeededFuture());
            } else {
              // found waiting translation request in queue
              log.debug("Found waiting translation.");
              log.debug(translationRequest.encode());

              // get necessary fields
              final String trId = translationRequest.getString("tr_id");
              final String sourceLanguage = translationRequest.getString("original_language");
              final JsonArray targetLanguages = new JsonArray(translationRequest.getString("target_languages").replace('\'', '\"'));
              final JsonObject dataDict = new JsonObject(translationRequest.getString("data_dict"));

              // start translation process for every snippet in translation request
              this.startTranslationProcess(trId, sourceLanguage, targetLanguages, dataDict, sendResult -> {
                if (sendResult.succeeded()) {
                  resultHandler.handle(Future.succeededFuture());
                } else {
                  resultHandler.handle(Future.failedFuture(sendResult.cause()));
                }
              });
            }
          } else {
            log.error("Could not get an unsended translation request from database.");
            resultHandler.handle(Future.failedFuture(dbResult.cause()));
          }
        });
      } else {
      log.debug("Enough active translations at this moment.");
      resultHandler.handle(Future.succeededFuture());
    }
    });
    return this;
  }

  @Override
  public TranslationService checkTranslationStatus(String trId, Handler<AsyncResult<Void>> resultHandler) {
    // Check the number of awaited translations
    translationDb.getNumTranslations(trId, numResult -> {
      if (numResult.succeeded()) {
        // Check if translation is outdated
        if (numResult.result() == null) {
          log.debug("Receive translation which is no longer in processed");
          translationDb.deleteTranslations(trId, deleteResult -> {
            if (deleteResult.failed()) {
              log.warn("Could not delete outdated translation from relation translation", deleteResult.cause());
              resultHandler.handle(Future.failedFuture(deleteResult.cause()));
            } else {
              resultHandler.handle(Future.succeededFuture());
            }
          });
          return;
        }

        final int awaitedTranslationCount = numResult.result().getInteger("num_translations");

        // Check the number of actual received translations
        translationDb.getActualNumTranslations(trId, actualNumResult -> {
          if (actualNumResult.succeeded()) {
            final int actualTranslationCount = actualNumResult.result().getInteger("count");

            // check if translation completed
            if (awaitedTranslationCount == actualTranslationCount && !completeTranslations.contains(trId)) {
              completeTranslations.add(trId);
              resultHandler.handle(Future.succeededFuture());

              // translation process completed
              translationDb.getTranslations(trId, translationResult -> {
                this.sendTranslationToRequester(trId, translationResult.result(), sendingResult -> {
                  if (sendingResult.succeeded()) {
                    translationDb.deleteActiveTranslation(trId, deleteResult -> {});
                  }
                });
              });
            } else {
              resultHandler.handle(Future.failedFuture("Translation not complete yet."));
            }
          }
        });
      }
    });
    return this;
  }

  private void sendTranslationToRequester(String trId, JsonArray translations, Handler<AsyncResult<Void>> resultHandler) {
    this.translationDb.getTranslationRequest(trId, dbResult -> {
      if (dbResult.succeeded()) {
        JsonObject result = dbResult.result();
        JsonObject response = this.buildResponseJson(trId, result.getString("original_language"), translations);

        // send translations back now / end of process
        String url = result.getString("callback_url");
        String auth = result.getString("callback_auth");
        WebClient client = WebClient.create(this.vertx);
        client
          .postAbs(url)
          .putHeader("Content-Type", "application/json")
          .putHeader("Authorization", auth)
          .sendJsonObject(response, ar -> {
            if (ar.succeeded()) {
              log.info("Send translation back to requester.");
              log.debug(response.toString());
              this.removeTranslation(result);
              completeTranslations.remove(trId);
              resultHandler.handle(Future.succeededFuture());
            } else {
              log.error("Could not send translation back to requester.", ar.cause());
              resultHandler.handle(Future.failedFuture(ar.cause()));
            }
          });

      } else {
        log.error("Could not get translation request from database.");
        resultHandler.handle(Future.failedFuture(dbResult.cause()));
      }
    });
  }

  private void startTranslationProcess(String trId, String originalLanguage, JsonArray targetLanguages, JsonObject dataDict, Handler<AsyncResult<Void>> resultHandler) {
    translationDb.sendedTranslationRequest(trId, changeResult -> {
      if (changeResult.succeeded()) {
        this.translationDb.insertActiveTranslation(trId, insertResult -> {
          if (insertResult.succeeded()) {
            log.info("New active translations: " + trId);
            dataDict.forEach(snippet -> {
              JsonObject json = this.buildJson(trId, snippet.getValue().toString(), originalLanguage, targetLanguages, snippet.getKey());
              log.debug(json.encode());
              this.sendTranslationRequestToEtranslation(json, sendResult -> {
                if (sendResult.succeeded()) {
                  resultHandler.handle(Future.succeededFuture());
                } else {
                  resultHandler.handle(Future.failedFuture(sendResult.cause()));
                }
              });
            });
          } else {
            log.error("Could not create a new active translation.");
          }
        });
      }
    });
  }

  private JsonObject buildResponseJson(String trId, String originalLanguage, JsonArray translations) {
    JsonObject response = new JsonObject().put("id", trId).put("original_language", originalLanguage);
    JsonObject translation = new JsonObject();
    translations.forEach(t -> {
      JsonArray part = (JsonArray) t;
      if (translation.containsKey(part.getString(1))) {
        translation.getJsonObject(part.getString(1)).put(part.getString(2), part.getString(3));
      } else {
        translation.put(part.getString(1), new JsonObject().put(part.getString(2), part.getString(3)));
      }
    });
    response.put("translation", translation);
    return response;
  }

  private void removeTranslation(JsonObject translationRequest) {
    String trId = translationRequest.getString("tr_id");
    String transmissionDate = translationRequest.getString("transmission_date");
    String finishedDate = LocalDateTime.now().toString();
    int duration = -1;
    String originalLanguage = translationRequest.getString("original_language");
    int numTargetLanguages = translationRequest.getInteger("num_translations");
    translationDb.deleteTranslationRequest(trId, ar -> {
      if (ar.failed()) {
        log.error("Could not delete translation request from database.");
      }
    });
    translationDb.deleteTranslations(trId, ar -> {
      if (ar.failed()) {
        log.error("Could not delete received translation from database.");
      }
    });
    translationDb.saveFinishedTranslation(trId, transmissionDate, finishedDate, duration, originalLanguage, numTargetLanguages, ar -> {
      if (ar.failed()) {
        log.error("Could not insert statistics about finished translations.");
      }
    });
  }

  private void sendTranslationRequestToEtranslation(JsonObject body, Handler<AsyncResult<Void>> resultHandler) {
    String url = this.config.getString(CONFIG_ETRANSLATION_URL);
    String applicationName = this.config.getString(CONFIG_ETRANSLATION_APP);
    String password = this.config.getString(CONFIG_ETRANSLATION_PASSWORD);
    this.vertx.executeBlocking(future -> {
      // crappy old code from eTranslation (with using apache http client)
      try {
        DefaultHttpClient client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(applicationName, password));
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(body.encode(), ContentType.APPLICATION_JSON.getMimeType(), "UTF-8"));
        HttpClientParams.setRedirecting(post.getParams(), false);
        HttpResponse response = client.execute(post);
        log.debug("eTranslation Response: " + response.getStatusLine());
        future.complete();
        resultHandler.handle(Future.succeededFuture());
      } catch (Exception e) {
        // TODO intelligent exception handling for this crappy code
        resultHandler.handle(Future.failedFuture(e.getMessage()));
      }
    }, resultHandler);

  }

  private JsonObject buildJson(String trId, String textToTranslate, String sourceLanguage, JsonArray targetLanguages, String textId) {
    JsonObject json = new JsonObject();
    json
      .put("priority", 0)
      .put("externalReference", trId + "+++" + textId)
      .put("callerInformation", new JsonObject()
        .put("application", this.config.getString(CONFIG_ETRANSLATION_APP))
        .put("username", this.config.getString(CONFIG_ETRANSLATION_USER)))
      .put("textToTranslate", textToTranslate)
      .put("sourceLanguage", sourceLanguage)
      .put("targetLanguages", targetLanguages)
      .put("domain", "SPD")
      .put("requesterCallback", this.config.getString(CONFIG_ETRANSLATION_CALLBACK) + "/et_success_handler")
      .put("errorCallback", this.config.getString(CONFIG_ETRANSLATION_CALLBACK) + "/et_error_handler")
      .put("destinations", new JsonObject());
    return json;
  }
}
