(ns lufs-bot.handling
  (:require
    [tg-bot-api.telegram :as telegram]
    [clojure.java.io :as io]
    [lufs-clj.core :as lufs]
    [me.raynes.conch :as conch]))


(defmacro with-safe-log
  "
  A macro to wrap Telegram calls (prevent the whole program from crushing).
  "
  [& body]
  `(try
     ~@body
     (catch Throwable e#
       (println (ex-message e#)))))


(defn regex-file-seq
  "Lazily filter a directory based on a regex."
  [re dir]
  (filter #(re-find re (.getPath %)) (file-seq dir)))


(defn slurp-delete
  [path]
  
  (let [res (slurp path)]
    
    (clojure.java.io/delete-file path)
    res))

(defn true-peak
  "measures true peak using ffmpeg
   will fix later"
  [filename]
  
  (conch/with-programs [ffmpeg]
    
    (ffmpeg 
      "-nostats"
      "-i" filename
      "-filter_complex" "ebur128=peak=true"
      "-f" "null"
      "-loglevel" "panic"
      "-report"
      "-"))
  (->> (regex-file-seq #"ffmpeg" (clojure.java.io/file "."))
    first
    slurp-delete
    (re-find #"Peak:\s*[-+]?[0-9]*(\.[0-9]+)*\ dBFS")
    first
    (re-find #"[+-]?([0-9]*[.])?[0-9]+")
    first
    parse-double))


(def LOADING "⏳")


(defn copy [uri file]
  (with-open [in (clojure.java.io/input-stream uri)
              out (clojure.java.io/output-stream file)]
    (clojure.java.io/copy in out)
    file))


(defn entry->str
  [e]
  (let [k (first e)
        v (second e)]
    (if (not= k LOADING)
      (str (name k) 
        (reduce str (repeat (- 12 
                              (count (name k)))
                              " "))
        (reduce str (repeat (- 6
                              (count (format "%.1f" (double v))))
                              " "))
        
        (format "%.1f" (double v)) "\n")
      (str  k "\n"))))


(defn parse-lufses
  [lufses-map]
  (reduce str (concat "```\n" (map entry->str (vec lufses-map)) "```")))


(defn the-handler 
  "Bot logic here"
  [config {:keys [message]}]
  
  
  (cond
    
    (and 
      (some? (:audio message))
      #_(> 20000000 (get-in message [:audio :file_size])))
    (let [file
        (->
          (with-safe-log
            (telegram/get-file 
            config
            (get-in message [:audio :file_id])))
          :file_path
          (copy "temp"))
        
        
        lufses
        {LOADING nil}
        
        
        sent-message
        (with-safe-log
          (telegram/send-message 
            config
            (get-in message [:chat :id])
            (parse-lufses lufses)
            {:parse-mode "markdown"
             :reply-to-message-id (:message_id message)}))
        
        edit
        (fn [m]
          (with-safe-log
            (telegram/edit-message-text
             config
             (get-in message [:chat :id])
             (:message_id sent-message)
             m
             {:parse-mode "markdown"})))
        
        lufses
        (assoc lufses 
          :Integrated
          (lufs/integrated file))
        
        edited-message
        (edit (parse-lufses lufses))
        
        lufses
        (assoc lufses
          :Short-Term
          (lufs/short-term file))
        
        edited-message
        (edit (parse-lufses lufses))
        
        lufses
        (assoc lufses
          :Momentary
          (lufs/momentary file))
        
        edited-message
        (edit (parse-lufses lufses))
        
        lufses
        (assoc lufses
          :LRA
          (lufs/lra file))
        
        edited-message
        (edit (parse-lufses lufses))
          
        lufses
        (assoc lufses
          :True-Peak
          (true-peak file))
        
        edited-message
        (edit (parse-lufses lufses))
        
        lufses
        (dissoc lufses LOADING)
        
        edited-message
        (edit (parse-lufses lufses))])
    
    
    
    (nil? (:audio message))
    (with-safe-log
      (telegram/send-message
            config
            (get-in message [:chat :id])
            "Пришлите аудио, и я посчитаю его громкость"))
    
    #_(<= 20000000 (get-in message [:audio :file_size]))
    #_(telegram/send-message 
          config
          (get-in message [:chat :id])
          "Файл должен быть меньше 20-ти мегабайт")))









(comment
  
  

  
  
  
  
  )










