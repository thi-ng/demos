(.importScripts js/self "base.js")

(ns meshworker
  (:require-macros
   [cljs-log.core :refer [debug info warn]])
  (:require
   [thi.ng.math.core :as m]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.mesh.io :as mio]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.strf.core :as f]))

(defn load-binary
  [uri onload onerror]
  (let [xhr (js/XMLHttpRequest.)]
    (.open xhr "GET" uri true)
    (set! (.-responseType xhr) "arraybuffer")
    (set! (.-onload xhr)
          (fn [e]
            (if-let [buf (.-response xhr)]
              (onload buf)
              (when onerror (onerror xhr e)))))
    (.send xhr)))

(defn build-mesh
  [buf]
  (let [t0       (f/timestamp)
        mesh     (mio/read-stl
                  (mio/wrapped-input-stream buf)
                  #(glm/gl-mesh % #{:fnorm}))
        bounds   (g/bounds mesh)
        tx       (-> mat/M44
                     (g/scale (/ 1.0 (-> bounds :size :y)))
                     (g/translate (m/- (g/centroid bounds))))
        vertices (-> mesh .-vertices .-buffer)
        fnormals (-> mesh .-fnormals .-buffer)
        num      (.-id mesh)]
    (debug (- (f/timestamp) t0) "ms," num "triangles")
    (.postMessage
     js/self
     #js [vertices fnormals num tx]
     #js [vertices fnormals])))

(defn load-mesh
  [msg]
  (load-binary
   (.-data msg)
   build-mesh
   #(warn "error loading mesh: " (.-data msg))))

(set! (.-onmessage js/self) load-mesh)
