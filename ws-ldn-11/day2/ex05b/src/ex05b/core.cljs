(ns ex05b.core
  (:require-macros
   [reagent.ratom :refer [reaction]])
  (:require
   [thi.ng.math.core :as m :refer [PI HALF_PI TWO_PI]]
   [thi.ng.color.core :as col]
   [thi.ng.geom.core :as g]
   [thi.ng.geom.vector :as v]
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.gl.core :as gl]
   [thi.ng.geom.gl.webgl.constants :as glc]
   [thi.ng.geom.gl.webgl.animator :as anim]
   [thi.ng.geom.gl.shaders :as sh]
   [thi.ng.geom.gl.glmesh :as glm]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex :as vertex]
   [reagent.core :as reagent]))

(defonce app (reagent/atom {}))

(def shader-spec
  {:vs
   (glsl/assemble
    (glsl/glsl-spec
     [vertex/surface-normal]
     "void main(){
      vec4 worldPos = model * vec4(position, 1.0);
      vec4 eyePos = view * worldPos;
      vEyePos = eyePos.xyz;
      vNormal = surfaceNormal(normal, normalMat);
      vLightPos = (view * vec4(lightPos, 1.0)).xyz;
      gl_Position = proj * eyePos;
  }"))
   :fs
   "void main(){
      vec3 L = normalize(vLightPos - vEyePos);
      vec3 N = normalize(vNormal);
      vec3 col = col0 * clamp(dot(N, L), 0.0, 1.0) + col1 * (1.0 - abs(dot(N, L))) + col2 * clamp(dot(-N,L), 0.0, 1.0);
      gl_FragColor=vec4(col, 1.0);
  }"
   :uniforms {:view      :mat4
              :proj      :mat4
              :model     [:mat4 M44]
              :normalMat [:mat4 (gl/auto-normal-matrix :model :view)]
              :col0      [:vec3 [0.3 0.3 0.3]]
              :col1      [:vec3 [1 1 1]]
              :col2      [:vec3 [0 0 0]]
              :lightPos  [:vec3 [0 1 2]]}
   :attribs {:position [:vec3 0]
             :normal   [:vec3 1]}
   :varying {:vNormal   :vec3
             :vEyePos   :vec3
             :vLightPos :vec3}
   :state    {:depth-test true}})

(def meshes
  [["../assets/suzanne.stl" "Blender Suzanne (788 KB)"]
   ["../assets/deadpool.stl" "Deadpool (2 MB)"]
   ["../assets/voxel.stl" "Voxel (11.2 MB)"]])

(defn trigger-mesh-change!
  [uri]
  (swap! app assoc :selected-mesh uri)
  (.postMessage (:worker @app) uri))

(defn receive-mesh!
  [msg]
  (let [[vertices fnormals id tx] (.-data msg)
        num   (* id 9)
        mesh  (thi.ng.geom.gl.glmesh.GLMesh.
               (js/Float32Array. vertices 0 num)
               (js/Float32Array. fnormals 0 num)
               nil nil nil id
               #{:fnorm})
        model (-> mesh
                  (gl/as-gl-buffer-spec {})
                  (assoc :shader   (:shader @app)
                         :uniforms (assoc (:uniforms @app) :model tx))
                  (gl/make-buffers-in-spec (:gl @app) glc/static-draw))]
    (swap! app assoc
           :mesh     mesh
           :model    model)))

(defn init-gl
  [this]
  (let [gl       (gl/gl-context (reagent/dom-node this))
        vport    (gl/get-viewport-rect gl)
        shader   (sh/make-shader-from-spec gl shader-spec)
        uniforms {:model         mat/M44
                  :proj          (mat/perspective 60 vport 0.1 10)
                  :view          (mat/look-at (v/vec3 0 0 1.25) (v/vec3) v/V3Y)
                  :lightPos      (v/vec3 0.1 0 1)}]
    (swap! app assoc
           :gl       gl
           :viewport vport
           :shader   shader
           :uniforms uniforms)))

(defn update-gl
  [this]
  (fn [t frame]
    (let [{:keys [gl viewport model model-tx]} @app]
      (when model
        (doto gl
          (gl/set-viewport viewport)
          (gl/clear-color-and-depth-buffer (col/rgba 0.3 0.3 0.3) 1)
          (gl/draw-with-shader
           (-> model
               (update-in [:uniforms :model] #(m/* (g/rotate-y mat/M44 t) %)))))))
    (:active (reagent/state this))))

(defn gl-component
  [props]
  (reagent/create-class
   {:component-did-mount
    (fn [this]
      (reagent/set-state this {:active true})
      ((:init props) this)
      (anim/animate ((:loop props) this)))
    :component-will-unmount
    (fn [this]
      (reagent/set-state this {:active false}))
    :reagent-render
    (fn [_]
      [:canvas
       (merge
        {:width (.-innerWidth js/window)
         :height (.-innerHeight js/window)}
        props)])}))

(defn mesh-selecta
  []
  (let [sel (reaction (:selected-mesh @app))]
    (fn []
      [:select
       {:default-value @sel
        :on-change     #(trigger-mesh-change! (-> % .-target .-value))}
       [:option "Choose:"]
       (for [[val label] meshes] [:option {:key val :value val} label])])))

(defn app-component
  []
  [:div
   [:div [mesh-selecta]]
   [gl-component {:init init-gl :loop update-gl}]])

(defn main
  []
  (let [worker (js/Worker. "js/meshworker.js")]
    (set! (.-onmessage worker) receive-mesh!)
    (swap! app assoc :worker worker)
    (reagent/render-component
     [app-component]
     (.getElementById js/document "app"))))

(main)
