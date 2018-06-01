(ns thi.ng.geom.webgl.example05
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.core.matrix :as mat :refer [M44]]
   [thi.ng.geom.aabb :as a]
   [thi.ng.geom.plane :as pl]
   [thi.ng.geom.core.intersect :as intersect]
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.animator :as anim]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.geom.webgl.shaders.phong :as phong]))

(defn raycast
  [p eye ground back]
  (let [dir (g/- p eye)
        i1  (:p (g/intersect-ray ground eye dir))
        i2  (:p (g/intersect-ray back eye dir))]
    (if (< (g/dist-squared eye i1) (g/dist-squared eye i2)) i1 i2)))

(defn ^:export demo
  []
  (let [gl         (gl/gl-context "main")
        view-rect  (gl/get-viewport-rect gl)
        shader     (sh/make-shader-from-spec gl phong/shader-spec)
        eye        (vec3 1 2 6)
        target     (vec3 0 0.6 0)
        up         v/V3Y
        size       3
        ground-y   -0.55
        uniforms   {:proj      (gl/perspective 45 view-rect 0.1 10.0)
                    :model     M44
                    :shininess 1000
                    :lightPos  (vec3 -1 2 0)}
        box        (-> (a/aabb 1)
                       (g/center)
                       (g/as-mesh)
                       (gl/as-webgl-buffer-spec {})
                       (buf/make-attribute-buffers-in-spec gl gl/static-draw)
                       (assoc :shader shader :uniforms (assoc uniforms :diffuseCol [1 0 1])))
        ground     (pl/plane-with-point (vec3 0 ground-y 0) v/V3Y)
        back       (pl/plane-with-point (vec3 0 0 (* -0.5 size)) v/V3Z)
        planes     (-> (g/as-mesh back {:size size})
                       (g/translate (vec3 0 (+ (* 0.5 size) ground-y) 0))
                       (g/into (g/as-mesh ground {:size size}))
                       (gl/as-webgl-buffer-spec {})
                       (buf/make-attribute-buffers-in-spec gl gl/static-draw)
                       (assoc :shader shader :uniforms uniforms))
        state      (volatile! {:mpos (g/centroid view-rect) :update-ray true})
        update-pos #(vswap! state assoc
                            :mpos (vec2 (.-clientX %) (.-clientY %))
                            :update-ray true)]
    (.addEventListener js/window "mousemove" update-pos)
    (.addEventListener js/window "touchmove" #(do (.preventDefault %) (update-pos (aget (.-touches %) 0))))
    (anim/animate
     (fn [[t frame]]
       (let [eye  (g/rotate-y eye (Math/sin t))
             view (mat/look-at eye target up)
             isec (if (:update-ray @state)
                    (let [p (-> (vec3 (:mpos @state) 0)
                                (mat/unproject-point (g/invert (g/* (:proj uniforms) view)) view-rect)
                                (raycast eye ground back))]
                      (vswap! state assoc :isec p :update-ray false) p)
                    (:isec @state))]
         (gl/set-viewport gl view-rect)
         (gl/clear-color-buffer gl 0.52 0.5 0.5 1)
         (gl/enable gl gl/depth-test)
         (phong/draw gl (assoc-in planes [:uniforms :view] view))
         (phong/draw gl (update box :uniforms merge {:model (g/translate M44 isec) :view view})))
       true))
    state))
