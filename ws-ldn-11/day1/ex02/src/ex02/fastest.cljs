(ns ex02.fastest
  (:require-macros
   [thi.ng.math.macros :as mm])
  (:require
   [ex02.state :as state]
   [ex02.utils :as utils]
   [thi.ng.typedarrays.core :as ta]
   [reagent.core :as r]))

(defn sum-neighbors
  "Returns number of active neighbours for a cell at x;y using
  thi.ng.math macro to compute sum."
  [grid idx stride]
  (let [t (- idx stride)
        b (+ idx stride)]
    (mm/add
     (aget grid (- t 1))
     (aget grid t)
     (aget grid (+ t 1))
     (aget grid (- idx 1))
     (aget grid (+ idx 1))
     (aget grid (- b 1))
     (aget grid b)
     (aget grid (+ b 1)))))

(defn life-step
  "Computes new state for a single cell."
  [grid idx stride]
  (let [neighbors (sum-neighbors grid idx stride)]
    (if (pos? (aget grid idx))
      (if (or (== neighbors 2) (== neighbors 3)) 1 0)
      (if (== neighbors 3) 1 0))))

(defn life
  "Computes next generation of entire cell grid."
  [w h [old new]]
  (let [w' (- w 1)
        h' (- h 2)]
    (loop [idx (+ w 1), x 1, y 1]
      (if (< x w')
        (do
          (aset new idx (life-step old idx w))
          (recur (inc idx) (inc x) y))
        (if (< y h')
          (recur (+ idx 2) 1 (inc y))
          [new old])))))

(defn draw
  "Visualizes grid state in given canvas context & image data buffer."
  [ctx img len [grid :as state]]
  (let [pixels (-> img .-data ta/uint32-view)]
    (loop [i 0, idx 0]
      (if (< i len)
        ;; byteorder: ABGR
        (do (aset pixels idx (if (pos? (aget grid i)) 0xff00ffff 0xff000000))
            (recur (inc i) (+ idx 1)))
        (do (.putImageData ctx img 0 0)
            state)))))

(defn prepare-image
  "Creates an ImageData object for given canvas context and fills it
  with opaque black."
  [ctx width height]
  (let [img (.createImageData ctx width height)]
    (.fill (ta/uint32-view (.-data img)) (int 0xff000000))
    img))

(defn init
  [this props]
  (let [{:keys [width height]} props
        num                    (* width height)
        grid                   (->> #(if (< (rand) 0.5) 1 0)
                                    (repeatedly num)
                                    ta/uint8)
        grid2                  (ta/uint8 num)
        ctx                    (.getContext (r/dom-node this) "2d")]
    (swap! state/app merge
           {:grid      [grid grid2]
            :pixels    (prepare-image ctx width height)
            :num-cells num})))

(defn redraw
  [this props]
  (let [{:keys [width height]}     props
        {:keys [pixels num-cells]} @state/app
        canvas                     (r/dom-node this)
        ctx                        (.getContext canvas "2d")]
    (let [[avg grid] (utils/run-with-timer
                      #(->> (:grid @state/app)
                            (life width height)
                            (draw ctx pixels num-cells)))]
      (swap! state/app assoc :grid grid :avg avg))))
