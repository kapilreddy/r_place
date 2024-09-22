(ns r-place-backend.core
  (:require [org.httpkit.server :as http-kit]
            [ring.util.response :refer [resource-response]]
            [compojure.route :refer [files not-found]]
            [compojure.core :refer [defroutes GET POST DELETE ANY context]]))

(defn chat-handler [req]
  (http-kit/with-channel req channel              ; get the channel
    ;; communicate with client using method defined above
    (http-kit/on-close channel (fn [status]
                        (println "channel closed")))
    (if (http-kit/websocket? channel)
      (println "WebSocket channel")
      (println "HTTP channel"))
    (http-kit/on-receive channel (fn [data]       ; data received from client
           ;; An optional param can pass to send!: close-after-send?
           ;; When unspecified, `close-after-send?` defaults to true for HTTP channels
           ;; and false for WebSocket.  (send! channel data close-after-send?)
                          (http-kit/send! channel data))))) ; data is sent directly to the client

(defn show-landing-page [req] ;; ordinary clojure function, accepts a request map, returns a response map
  ;; return landing page's html string. Using template library is a good idea:
  ;; mustache (https://github.com/shenfeng/mustache.clj, https://github.com/fhd/clostache...)
  ;; enlive (https://github.com/cgrand/enlive)
  ;; hiccup(https://github.com/weavejester/hiccup)
  (resource-response "templates/index.html"))


(defroutes all-routes
  (GET "/" [] show-landing-page)

  (GET "/ws" [] chat-handler)     ;; websocket
  (files "/static/" {:root "resources"}) ;; static file url prefix /static, in `public` folder
  (not-found "<p>Page not found.</p>")) ;; all other, return 404


(defn -main []
  (http-kit/run-server all-routes {:port 8080}))
