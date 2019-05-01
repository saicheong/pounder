(defproject pounder "0.1.0-SNAPSHOT"
  :description "Application for stress/load testing a webservice"
  :url "https://github.com/saicheong/pounder"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-gatling "0.13.0"]
                 [clj-http "3.6.1"]]
  :main ^:skip-aot pounder.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
