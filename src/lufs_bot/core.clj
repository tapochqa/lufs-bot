(ns lufs-bot.core 
  (:gen-class)
  (:require
    [lufs-bot.polling  :as polling]
    [lufs-bot.lambda   :as lambda]
    [tg-bot-api.telegram :as telegram]
    [clojure.string    :as str]
    [cheshire.core     :as json]))


(defn polling
  [config]
  (polling/run-polling config))

(defn lambda
  [config]
  (-> (lambda/->request)
      (lambda/handle-request! config)
      (lambda/response->)))

(defn -main
  [my-token]
  
  (let [config 
        { :test-server false
          :local-server "http://95.142.47.99:9955"
          :token my-token
          :polling {:update-timeout 1000}}]
  (polling/run-polling config)
  #_(lambda config)
  ))


(comment
  
   (binding [*in* (-> "trigger-request.json"
                 clojure.java.io/resource
                 clojure.java.io/reader)]
     
     (-main "...:..."))
  
  
    (-main "...")
  
  (def CONFIG
    {:token "..."
     :local-server "..."})

  (:file_path
    (telegram/get-file
    CONFIG
    "..."))
  
  )
