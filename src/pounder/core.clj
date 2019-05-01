(ns pounder.core
  (:require [clj-gatling.core :as gatling]
            [clojure.core.async :refer [chan go >!]]
            [org.httpkit.client :as hk]
            [clj-http.client :as ch]
            [clojure.java.io :as io])
  (:gen-class))

(defn setup [ctx]
  (println "Setting up simulation run")
  ;; load sample messages into ctx
  (assoc ctx :msgs (with-open [rdr (io/reader (io/resource "msgs.txt"))]
                     (vec (line-seq rdr)))
             :url "http://localhost:3000/call-check"))

(defn cleanup [ctx]
  (println "Finishing up simulation run"))

(defn call-with-httpkit [{:keys [msgs url]}]
  (let [msg (nth msgs (rand-int (count msgs)))
        options {:headers {"content-type" "application/json"}}
        response (chan)
        check-status (fn [{:keys [status]}]
                       (go (>! response (= 200 status))))]
    (hk/post url (assoc options :body msg) check-status)
    response))

(defn call-with-clj-http [{:keys [msgs url]}]
  (let [msg (nth msgs (rand-int (count msgs)))
        options {:async? true
                 :content-type :json}
        response (chan)
        check-status (fn [{:keys [status]}]
                       (go (>! response (= 200 status))))
        err (fn [ex] (go (>! response (str "exception:" (.getMessage ex)))))]
    (ch/post url (assoc options :body msg) check-status err)
    response))

(def simulation
  {:name "Base Simulation"
   :pre-hook setup
   :post-hook cleanup
   :scenarios [{:name "Base Scenario"
                :steps [{:name    "Call web-service"
                         :request call-with-httpkit}]}]})

(def simulation2
  {:name "Base Simulation"
   :pre-hook setup
   :post-hook cleanup
   :scenarios [{:name "Base Scenario"
                :steps [{:name    "Call web-service"
                         :request call-with-clj-http}]}]})

(defn -main [num-users num-requests & opts]
  (gatling/run simulation
               {:concurrency (read-string num-users)
                :root "tmp"
                :requests (read-string num-requests)}))

(comment
  (require 'pounder.core :reload)

  (gatling/run simulation
               {:concurrency 1
                :root "tmp"
                :requests 10})

  (gatling/run simulation2
               {:concurrency 1
                :root "tmp"
                :requests 10})

  (call-with-clj-http nil)

  (require '[clojure.java.io :as io])

  (def msg (slurp (io/resource "msgs.txt")))

  )