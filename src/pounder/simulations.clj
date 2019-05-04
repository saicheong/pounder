(ns pounder.simulations
  (:require [clj-gatling.core :as gatling]
            [pounder.core :as core]))

(defn setup [ctx]
  (println "Setting up simulation run")
  ;; load sample messages into ctx
  (assoc ctx :msgs (core/load-resource "msgs.txt")
             :url "http://localhost:3000/call-check"))

(defn cleanup [ctx]
  (println "Finishing up simulation run"))

(def simulation
  {:name "Using http-kit"
   :pre-hook setup
   :post-hook cleanup
   :scenarios [{:name "Base Scenario"
                :steps [{:name    "Call web-service"
                         :request core/http-kit-post}]}]})

(def simulation2
  {:name "Using clj-http"
   :pre-hook setup
   :post-hook cleanup
   :scenarios [{:name "Base Scenario"
                :steps [{:name    "Call web-service"
                         :request core/clj-http-post}]}]})

(comment
  (clojure.core/require 'pounder.simulations :reload)

  (gatling/run simulation
               {:concurrency 1
                :root "tmp"
                :requests 5})

  (gatling/run simulation2
               {:concurrency 1
                :root "tmp"
                :requests 5})

)
