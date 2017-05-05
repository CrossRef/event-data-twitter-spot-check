(defproject event-data-twitter-spot-check "0.1.0-SNAPSHOT"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [event-data-common "0.1.25"]
                 [org.clojure/data.json "0.2.6"]
                 [crossref-util "0.1.10"]
                 [clj-http "3.4.1"]
                 [robert/bruce "0.8.0"]
                 [yogthos/config "0.8"]
                 [throttler "1.0.0"]
                 [twitter-api "1.8.0"]]
  :main ^:skip-aot event-data-twitter-spot-check.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
