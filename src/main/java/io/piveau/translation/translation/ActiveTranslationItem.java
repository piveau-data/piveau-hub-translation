package io.piveau.translation.translation;

import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ActiveTranslationItem {

  private static final Logger log = LoggerFactory.getLogger(ActiveTranslationItem.class);

  private String trId;
  private int numTranslations;
  private int receivedTranslation;
  private Set<String> targetLanguages;
  private LocalDateTime requestTime;

  public ActiveTranslationItem(String trId, int numTranslations, JsonArray targetLanguages) {
    this.trId = trId;
    this.numTranslations = numTranslations;
    this.receivedTranslation = 0;
    this.targetLanguages = this.translateArrayToSet(targetLanguages);
    this.requestTime = LocalDateTime.now();
  }

  public String getTrId() {
    return trId;
  }

  public int getNumTranslations() {
    return numTranslations;
  }

  public Set<String> getTargetLanguages() {
    return targetLanguages;
  }

  public void addTargetLanguage(String targetLanguage) {
    this.targetLanguages.add(targetLanguage);
  }

  public void removeTargetLanguage(String targetLanguage) {
    this.targetLanguages.remove(targetLanguage);
  }

  public int requestedTranslations() {
    return this.targetLanguages.size();
  }

  public void receivedTranslation() {
    ++this.receivedTranslation;
  }

  public int getReceivedTranslation() {
    return this.receivedTranslation;
  }

  public LocalDateTime getRequestTime() {
    return requestTime;
  }

  private Set<String> translateArrayToSet(JsonArray array) {
    HashSet<String> set = new HashSet<String>();
    Iterator iterator = array.iterator();
    while (iterator.hasNext()) {
      String language = (String) iterator.next();
      if (!set.contains(language)) {
        set.add(language);
      }
    }
    return set;
  }
}
