(ns thi.ng.geom.webgl.example01
  (:require-macros
   [thi.ng.math.macros :as mm])
  (:require
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.geom.webgl.utils :as glu]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.core.matrix :as mat :refer [M44]]
   [thi.ng.geom.types]
   [thi.ng.typedarrays.core :as arrays]
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.geom.webgl.shaders.basic :as basic]
   [thi.ng.geom.circle :as c]
   [thi.ng.geom.polygon :as poly]))

(defn init-stats
  []
  (let [stats (js/Stats.)
        sdom  (.call (aget stats "getDomElement") stats)]
    (.appendChild (.-body js/document) sdom)
    (.setAttribute sdom "class" "stats")
    stats))

(defn update-stats
 [stats]
 (.call (aget stats "update") stats))

(defn ^:export demo
  []
  (enable-console-print!)
  (let [gl         (gl/gl-context "main")
        view-rect  (gl/get-viewport-rect gl)
        shader1    (sh/make-shader-from-spec
                    gl (basic/make-shader-spec-2d false))
        shader2    (sh/make-shader-from-spec
                    gl (basic/make-shader-spec-2d true))
        teeth      20
        model      (-> (poly/cog 0.5 teeth [0.9 1 1 0.9])
                       (gl/as-webgl-buffer-spec {:normals false :single-color [1 0 0 1]})
                       (buf/make-attribute-buffers-in-spec gl gl/static-draw)
                       (assoc-in [:uniforms :proj] (gl/ortho view-rect))
                       (time))
        stats      (init-stats)]
    (anim/animate
     (fn [[t frame]]
       (gl/set-viewport gl view-rect)
       (gl/clear-color-buffer gl 1 0.98 0.95 1.0)
       ;; draw left polygon using color uniform (that's why we need to remove color attrib)
       (buf/draw-arrays-with-shader
        gl (-> model
               (assoc :shader  shader1)
               (update-in [:attribs] dissoc :color)
               (update-in [:uniforms] merge
                          {:model (-> M44 (g/translate -0.48 0 0) (g/rotate t))
                           :color [0 1 1 1]})))
       ;; draw right polygon using color attribs
       (buf/draw-arrays-with-shader
        gl (-> model
               (assoc :shader shader2)
               (assoc-in [:uniforms :model]
                         (-> M44 (g/translate 0.48 0 0) (g/rotate (- (+ t (/ HALF_PI teeth))))))))
       (update-stats stats)
       true))))
