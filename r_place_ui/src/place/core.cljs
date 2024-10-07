(ns place.core
  (:require [cljs.reader :as cljsr]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.style :as style]))


(def canvas (dom/getElement "paintCanvas"))
(def ctx (.getContext canvas "2d"))
(def tools
  {:brush (dom/getElement "brush")
   :eraser (dom/getElement "eraser")
   :color-picker (dom/getElement "colorPicker")
   :demo (dom/getElement "demo")})

(defonce app-state (atom {}))
;; Set canvas size

(defn set-zoom [new-scale]
  (swap! app-state assoc :scale new-scale)
  (style/setStyle canvas "transform" (str "scale(" new-scale ")"))
  (style/setStyle canvas "transformOrigin" "top left")
  (draw-grid))

(defn draw-grid []
  (set! (.-strokeStyle ctx) grid-color)
  (set! (.-lineWidth ctx) 0.5)
  (.beginPath ctx)
  (doseq [x (range 0 (.-width canvas) grid-size)]
    (.moveTo ctx x 0)
    (.lineTo ctx x (.-height canvas)))
  (doseq [y (range 0 (.-height canvas) grid-size)]
    (.moveTo ctx 0 y)
    (.lineTo ctx (.-width canvas) y))
  (.stroke ctx))

(defn draw-grid-pixel [grid-idx-x grid-idx-y color]
  (let [grid-x (* grid-idx-x grid-size)
        grid-y (* grid-idx-y grid-size)]
    (set! (.-fillStyle ctx) color)
    (.fillRect ctx grid-x grid-y grid-size grid-size)))

(defn canvas-coord->grid-coord
  [coord grid-size]
  (* (Math/floor (/ coord grid-size)) grid-size))

(defn draw-pixel [x y color]
  (let [grid-x (canvas-coord->grid-coord x grid-size)
        grid-y (canvas-coord->grid-coord y grid-size)]
    (def l* [grid-x grid-y])
    (set! (.-fillStyle ctx) color)
    (.fillRect ctx grid-x grid-y grid-size grid-size)))


(defn send-pixel!
  [x y color]
  (.send socket* (pr-str {:column x
                          :row y
                          :color color})))

(defn handle-canvas-click [e]
  (let [rect (.getBoundingClientRect canvas)
        scale (:scale @app-state)
        x (/ (- (.-clientX e) (.-left rect)) scale)
        y (/ (- (.-clientY e) (.-top rect)) scale)
        color (if (= (:current-tool @app-state) :eraser)
                "#FFFFFF"
                (:current-color @app-state))]
    (draw-pixel x y color)
    (send-pixel! (Math/floor (/ x grid-size))
                 (Math/floor (/ y grid-size))
                 color)
    (draw-grid)))

(events/listen canvas "click" handle-canvas-click)

(defn websocket-message-handler
    [data]
    (let [{:keys [column row color]} (cljsr/read-string data)]      
      (draw-grid-pixel column row color)))

(do 
  (set! (.-width canvas) 500)
  (set! (.-height canvas) 500)

  ;; Grid settings
  (def grid-size 20)
  (def grid-color "#808080")

  (def app-state (atom {:current-tool :brush
                        :current-color "#000000"
                        :scale 0.1}))

  ;; Set initial zoom
  (set-zoom 1)

  ;; Draw initial grid
  (draw-grid)
  


  (def socket* (setup-ws "ws://localhost:8080/ws"
                         (fn []
                           (js/console.log "Socket connected"))
                         (fn [e]
                           (js/console.log e.data)
                           (websocket-message-handler e.data)))))


(comment 
  (draw-grid-pixel 2 1 "red")
  (draw-grid-pixel 0 0 "red")

  (def data {:column 0
             :row 1
             :color "red"})
  )

(defn setup-ws
  [ws-endpoint on-open-fn on-message-fn]
  (let [socket (js/WebSocket. ws-endpoint)]
    (set! (.-onopen socket)
          on-open-fn)
    (set! (.-onmessage socket)
          on-message-fn)
    socket))


(def sample-data {:column 0
                  :row 1
                  :color "red"})




(comment
  (websocket-message-handler (pr-str sample-data))

  

  (.send socket* "pong")
  (events/listen canvas "touchstart"
                 (fn [e]
                   (.preventDefault e)
                   (handle-canvas-click (aget (.-touches e) 0))))

  (events/listen (:brush tools) "click" #(swap! app-state assoc :current-tool :brush))
  (events/listen (:eraser tools) "click" #(swap! app-state assoc :current-tool :eraser))
  (events/listen (:color-picker tools) "change"
                 #(swap! app-state assoc :current-color (.. % -target -value)))
  )

