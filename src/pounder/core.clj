(ns pounder.core
  (:require [clj-gatling.core :as gatling]
            [clojure.core.async :refer [chan go >! <!!]]
            [clojure.java.io :as io])
  (:gen-class))

(defn load-resource
  "Load resource from specified path and return the resources as a vector"
  [path]
  (with-open [rdr (io/reader (io/resource path))]
    (vec (line-seq rdr))))

(defn http-kit-post
  ([{:keys [url msgs]}]
   (let [msg (nth msgs (rand-int (count msgs)))]
     (http-kit-post url msg)))
  ([url msg]
   (let [options {:headers {"content-type" "application/json"}}
         response-chan (chan)
         response-handler (fn [{:keys [status]}]
                            (go (>! response-chan (= 200 status))))]
     (org.httpkit.client/post url (assoc options :body msg) response-handler)
     response-chan)))

(defn clj-http-post
  ([{:keys [url msgs]}]
   (let [msg (nth msgs (rand-int (count msgs)))]
     (clj-http-post url msg)))
  ([url msg]
   (let [options {:async?       true
                  :content-type :json}
         response-chan (chan)
         response-handler (fn [{:keys [status]}]
                            (go (>! response-chan (= 200 status))))
         error-handler (fn [ex]
                         (go (>! response-chan (str "exception:" (.getMessage ex)))))]
     (clj-http.client/post url (assoc options :body msg) response-handler error-handler)
     response-chan)))


(defn -main [& args]
  ;TODO
  )

(comment
  (require 'pounder.core :reload)

  (def url "http://localhost:3000/call-check")
  (def msgs (load-resource "msgs.txt"))
  (take 2 msgs)

  (<!! (http-kit-post url (first msgs)))
  (<!! (clj-http-post url (first msgs)))

  )