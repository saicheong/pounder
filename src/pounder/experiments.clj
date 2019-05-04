(ns pounder.experiments
  (:require [clj-gatling.core :as gatling]
            [clojure.core.async :refer [chan go >!]]
            [clojure.pprint :refer [pprint]]))

(defn setup [ctx]
  (println "\n\n===========================")
  (println "Setting up simulation run")
  (pprint ctx)
  (assoc ctx :setup-id "setup"))

(defn cleanup [ctx]
  (pprint ctx)
  (println "Finishing up simulation run")
  (println "===========================\n\n"))

(defn print-ctx [ctx]
  (locking *out* (println "User " (:user-id ctx) ": " ctx)))

(defn do-nothing-step
  "This do-nothing step is needed because of a bug such the ctx
  returned by the last step is ignored by the scenario. This
  do nothing step ensures that the actual last step ctx is picked
  up by the scenario post-hook"
  [ctx] [true ctx])

(defn debug-step [step-name]
  (fn [{:keys [user-id step-cnt] :as ctx}]
    (locking *out*
      (println "User " user-id ": " step-name))
    [true (assoc ctx :step-cnt (inc step-cnt))]))

(defn channel-step
  "A step that returns a channel"
  [step-name]
  (fn [{:keys [user-id step-cnt] :as ctx}]
    (locking *out*
      (println "User " user-id ": " step-name))
    (let [c (chan)]
      (go (>! c [true (assoc ctx :step-cnt (inc step-cnt))]))
      c)))

(def run-opts {:root "tmp"
               :error-file "tmp/error.log"
               :concurrency 1})

(defn run [sim opts]
  (gatling/run sim (merge run-opts opts)))

(comment
  (clojure.core/require '[pounder.experiments] :reload)

  (run {:name      "Single user simulation"
        :pre-hook  setup
        :post-hook cleanup
        :scenarios [{:name "Multi-steps"
                     :context {:step-cnt 0}
                     :post-hook print-ctx
                     :steps [{:name "step1"
                              :request (debug-step "=step1=")}
                             {:name "step2"
                              :request (channel-step "=step2=")}
                             {:name "step3"
                              :request (debug-step "=step3=")}
                             {:name "XXXX"
                              :request do-nothing-step}]}]}
       ; simulation context
       {:context {:env :repl}})

  "Lessons:
  1) To return context in request function - it must be returned
     in a tuple [result context]

  2) For async request - put the tuple (if need to return context
     into channel

  3) The context from simulation is available to the scenarios,
     and the context from scenarios (include simulation values)
     are available at each step.

  4) The context values at each step includes the context values
     from previous steps (which also include context values from
     scenario and simulation... really nice!

  5) When there are multiple threads calling writing to *out*,
     use the locking macro to hold a monitor on *out*"


  (run {:name      "Multi-user simulation"
        :pre-hook  setup
        :post-hook cleanup
        :scenarios [{:name      "Multi-steps"
                     :context   {:step-cnt 0}
                     :post-hook print-ctx
                     :steps     [{:name    "step1"
                                  :request (debug-step "=step1=")}
                                 {:name    "step2"
                                  :request (channel-step "=step2=")}
                                 {:name    "step3"
                                  :request (debug-step "=step3=")}
                                 {:name    "XXXX"
                                  :request do-nothing-step}]}]}
       {:context {:env :repl}
        :concurrency 2})

  "Lessons:
  1) Even though concurrency is set at 2 - gatling at times can run
     the scenarios more than 2 times. I have not figured out why."

  "TODO:
  1) What is the best way to do async calls?
     A) Using go block and channel
     B) Using go block alone

  2) ....

  "


  )