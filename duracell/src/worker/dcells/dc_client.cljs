(ns dcells.dc-client
  (:require-macros
    [aaworker.worker-macros :refer [deflpc!]])
  (:require
    [aaworker.api :as api]))

(def db (atom nil))

(defn start [databaseName]
  (let [indexedDB (.-indexedDB js/self)
        request (.open indexedDB databaseName)]
    (set! (.-onupgradeneeded request)
          (fn [event]
            (let [db (-> event .-target .-result)
                  object-store (.createObjectStore db "cells")])))
    (set! (.-onerror request)
          (fn [event]
            (aaworker.api/send-notice
              :alert
              (str "Unable to open indexedDB "
                   databaseName
                   (-> event .-target .-errorCode)))))
    (set! (.-onsuccess request)
          (fn [event]
            (reset! db (-> event .-target .-result))
            (api/process-requests)))))