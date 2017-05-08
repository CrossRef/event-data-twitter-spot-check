(ns event-data-twitter-spot-check.core
  (:require [throttler.core :refer [throttle-fn]]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [config.core :refer [env]]
            [robert.bruce :refer [try-try-again]]
            [twitter.oauth :as oauth]
            [twitter.callbacks :as callbacks]
            [twitter.callbacks.handlers :as handlers]
            [twitter.api.restful :as restful])
  (:import [twitter.callbacks.protocols SyncSingleCallback])
  (:gen-class))

(def processed-events-counter (atom 0))
(def deleted-events-counter (atom 0))

(def my-creds (oauth/make-oauth-creds (:twitter-api-key env)
                                (:twitter-api-secret env)
                                (:twitter-access-token env)
                                (:twitter-access-token-secret env)))

(def throttle-per-minute
  "Throttle maximum for /statuses/lookup API endpoint.
   This is stipulated in an elevated access agreement.
   Don't run this without confirming the rate limit."
   80)

; https://dev.twitter.com/rest/reference/get/statuses/lookup
(def ids-per-request 100)

(defn fetch-query-api
  "Fetch a lazy seq of all Twitter Events."
  ([] (fetch-query-api ""))
  ([cursor]
    (log/info "Fetch Query API cursor" cursor)
    (let [url (str (:query-api-base env) "/events?experimental=true&filter=source:twitter&cursor=" cursor)
          response (try-try-again {:sleep 30000 :tries 10} #(client/get url {:as :stream :timeout 900000}))
          body (json/read (io/reader (:body response)) :key-fn keyword)
          events (-> body :message :events)
          next-cursor (-> body :message :next-cursor)]
      (if next-cursor
        (lazy-cat events (fetch-query-api next-cursor))
        events))))

(defn events-in-batch-should-be-deleted
  [event-batch]
  (let [ids (set (map #(-> % :subj :alternative-id str) event-batch))
        response (try-try-again #(restful/statuses-lookup  :oauth-creds my-creds :params {:id (clojure.string/join "," ids) :map false}))        
        ; Whole response comes back as keywords, including Tweet ID keys. Convert these to strings.
        ; extant-tweet-ids (->> response :body :id keys (map name) set)
        extant-tweet-ids (->> response :body (map :id) (map str))
        deleted-tweet-ids (clojure.set/difference ids extant-tweet-ids)
        deleted-event-ids (keep #(when (-> % :subj :alternative-id str deleted-tweet-ids)
                                       (:id %)) event-batch)]

    (swap! processed-events-counter #(+ % (count event-batch)))
    (swap! deleted-events-counter #(+ % (count deleted-event-ids)))
    
    (log/info "Deleted Tweet IDs" deleted-tweet-ids)
    (log/info "Deleted Event IDs" deleted-event-ids)
    (log/info "Deleted" @deleted-events-counter "/" @processed-events-counter "events = " (float (/ @deleted-events-counter @processed-events-counter )))
  deleted-event-ids))

(def events-in-batch-should-be-deleted-throttled
  (throttle-fn events-in-batch-should-be-deleted throttle-per-minute :minute))

(defn -main
  [& args]
  (let [; Start-cursor can be nil.
        events (fetch-query-api (or (:start-cursor env) ""))
        ; Into chunks suitable for Twitter API.
        batches (partition-all ids-per-request events)
        ; Into seq of event-ids that have deleted tweets.
        deleted-event-ids (mapcat events-in-batch-should-be-deleted-throttled batches)]
    
    ; Append one by one. This is relatively low volume.
    (doseq [event-id deleted-event-ids]
      (spit "deleted-ids" (str event-id "\n") :append true))))

