(ns thi.ng.geom.webgl.shaders.lambert
  (:require
   [thi.ng.geom.core.matrix :refer [M44]]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex :as vertex]
   [thi.ng.glsl.lighting :as light]))

(defn- make-shader-spec
  [vs-src]
  {:vs (->> vs-src
            (glsl/glsl-spec-plain
             [vertex/mvp vertex/normal light/lambert light/lambert-abs])
            (glsl/assemble))
   :fs "void main(){gl_FragColor=vCol;}"
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :proj       :mat4
              :normalMat  :mat4
              :ambientCol [:vec3 [0 0 0]]
              :diffuseCol [:vec3 [1 1 1]]
              :lightCol   [:vec3 [1 1 1]]
              :lightDir   [:vec3 [0 0 1]]
              :alpha      [:float 1]}
   :attribs  {:position   :vec3
              :normal     :vec3}
   :varying  {:vCol       :vec4}
   :state    {:depth-test true}})

(def shader-spec
  (make-shader-spec
   (glsl/minified "
void main() {
  float lam = lambert(surfaceNormal(normal, normalMat), lightDir);
  vCol = vec4(ambientCol + diffuseCol * lightCol * lam, alpha);
  gl_Position = mvp(position, model, view, proj);
}")))

(def shader-spec-two-sided
  (make-shader-spec
   (glsl/minified "
void main() {
  float lam = lambertAbs(surfaceNormal(normal, normalMat), lightDir);
  vCol = vec4(ambientCol + diffuseCol * lightCol * lam, alpha);
  gl_Position = mvp(position, model, view, proj);
}")))

(defn draw
  [^WebGLRenderingContext gl spec]
  (buf/draw-arrays-with-shader gl (sh/inject-normal-matrix spec :model :view :normalMat)))
