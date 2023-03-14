(ns lufs-bot.handling
  (:require
    [tg-bot-api.telegram :as telegram]
    [clojure.java.io :as io]
    [lufs-clj.core :as lufs]))


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
      (> 20000000 (get-in message [:audio :file_size])))
    (let [file
        (->
          (telegram/get-file 
          config
          (get-in message [:audio :file_id]))
          :url
          (copy "temp"))
        
        
        lufses
        {LOADING nil}
        
        
        sent-message
        (telegram/send-message 
          config
          (get-in message [:chat :id])
          (parse-lufses lufses)
          {:parse-mode "markdown"
           :reply-to-message-id (:message_id message)})
        
        edit
        (fn [m]
          (telegram/edit-message-text
           config
           (get-in message [:chat :id])
           (:message_id sent-message)
           m
           {:parse-mode "markdown"}))
        
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
        (dissoc lufses LOADING)
        
        edited-message
        (edit (parse-lufses lufses))])
    
    
    
    (nil? (:audio message))
    (telegram/send-message 
          config
          (get-in message [:chat :id])
          "Пришлите аудио, и я посчитаю его громкость")
    
    (<= 20000000 (get-in message [:audio :file_size]))
    (telegram/send-message 
          config
          (get-in message [:chat :id])
          "Файл должен быть меньше 20-ти мегабайт"))

  )


