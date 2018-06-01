(ns thi.ng.geom.webgl.shaders.overlay
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.matrix :refer [M44]]
   [thi.ng.geom.rect :as r]
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.typedarrays.core :as arrays]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.math.core :as m]))

(def shader-spec
  {:vs (glsl/minified "
void main() {
  vUV = uv;
  gl_Position = proj * model * vec4(position, 0.0, 1.0);
}")
   :fs (glsl/minified "
void main() {
  gl_FragColor = texture2D(tex, vUV);
}")
   :uniforms {:proj       :mat4
              :model      [:mat4 M44]
              :tex        [:sampler2D 0]}
   :attribs  {:position   :vec2
              :uv         :vec2}
   :varying  {:vUV        :vec2}
   :state    {:depth-test false
              :blend true
              :blend-func [gl/src-alpha gl/one-minus-src-alpha]}})

(defn overlay-spec-from-rect
  ([^WebGLRenderingContext gl]
     (overlay-spec-from-rect gl (r/rect 1) true))
  ([^WebGLRenderingContext gl r init-shader?]
     (let [vbuf (arrays/float32 8)
           [a b c d] (g/vertices r)
           _ (g/copy-to-buffer [a b d c] vbuf 2 0)
           spec {:attribs  (buf/make-attribute-buffers
                            gl gl/static-draw
                            {:position {:data vbuf :size 2}
                             :uv       {:data (arrays/float32 [0 0, 1 0, 0 1, 1 1]) :size 2}})
                 :uniforms {:tex  0
                            :proj (gl/ortho)}
                 :mode     gl/triangle-strip
                 :num-vertices 4}]
       (if init-shader?
         (assoc spec :shader (sh/make-shader-from-spec gl shader-spec))
         spec))))

(defn draw-overlay2d
  [^WebGLRenderingContext gl {:keys [tex viewport pos width height] :as spec}]
  (let [[vw vh] (:size (or viewport (gl/get-viewport-rect gl)))
        x (m/map-interval (pos 0) 0 vw -1 1)
        y (m/map-interval (pos 1) 0 vh -1 1)
        s [(* 2.0 (/ width vw)) (* 2.0 (/ height vh))]
        spec (assoc-in spec [:uniforms :model] (-> M44 (g/translate x y 0) (g/scale s)))]
    (when tex
      (gl/bind tex (get-in spec [:uniforms :tex])))
    (buf/draw-arrays-with-shader gl spec)))
