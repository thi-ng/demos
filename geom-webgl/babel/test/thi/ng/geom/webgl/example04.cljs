(ns thi.ng.geom.webgl.example04
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v]
   [thi.ng.geom.core.matrix :as mat :refer [M44]]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.geom.webgl.shaders.basic :as basic]
   [thi.ng.typedarrays.core :as arrays]))

(defn colored-verts
  [faces col]
  (let [verts (apply concat faces)]
    [verts (repeat (count verts) col)]))

(defn flat-map
  [f coll] (flatten (map f coll)))

(defn ^:export demo
  []
  (let [gl        (gl/gl-context "main")
        view-rect (gl/get-viewport-rect gl)
        sides     (->> (a/aabb 1) (g/center) (g/faces) (map gu/tessellate-3))
        col-verts (map colored-verts sides [[1 0 0 1] [0 1 0 1] [0 0 1 1] [1 1 0 1] [1 0 1 1] [0 1 1 1]])
        verts     (flat-map first col-verts)
        cols      (flat-map second col-verts)
        model     (-> {:attribs      {:position {:data (arrays/float32 verts) :size 3}
                                      :color    {:data (arrays/float32 cols) :size 4}}
                       :uniforms     {:proj     (gl/perspective 45 view-rect 0.1 100.0)
                                      :view     (mat/look-at (v/vec3 0 0 2) (v/vec3) v/V3Y)}
                       :mode         gl/triangles
                       :num-vertices (/ (count verts) 3)
                       :shader       (->> (basic/make-shader-spec-3d true)
                                          (sh/make-shader-from-spec gl))}
                      (buf/make-attribute-buffers-in-spec gl gl/static-draw))]
    (anim/animate
     (fn [[t frame]]
       (gl/set-viewport gl view-rect)
       (gl/clear-color-buffer gl 1 1 1 1)
       (gl/enable gl gl/depth-test)
       (buf/draw-arrays-with-shader
        gl (assoc-in model [:uniforms :model] (-> M44 (g/rotate-x t) (g/rotate-y (* t 2)))))
       true))))
