(ns thi.ng.geom.webgl.shaders.xray
  (:require
   [thi.ng.geom.core.matrix :refer [M44]]
   [thi.ng.geom.webgl.core :as gl]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex :as vert]))

(def shader-spec
  {:vs (glsl/assemble
        (glsl/glsl-spec
         [vert/normal]
         "void main() {
            vIncident = view * model * vec4(position, 1.0);
            vNormal = surfaceNormal(normal, normalMat);
            gl_Position = proj * vIncident;
          }"))
   :fs (glsl/minified
        "void main() {
           float opac = abs(dot(normalize(-vNormal), normalize(-vIncident.xyz)));
           opac = 1.0 - pow(opac, alpha);
           gl_FragColor = vec4(lightCol * opac, opac);
         }")
   :uniforms {:model      [:mat4 M44]
              :view       :mat4
              :normalMat  :mat4
              :proj       :mat4
              :lightCol   [:vec3 [1 1 1]]
              :alpha      [:float 0.5]}
   :attribs  {:position   :vec3
              :normal     :vec3}
   :varying  {:vIncident  :vec4
              :vNormal    :vec3}
   :state    {:depth-test false
              :blend true
              :blend-func [gl/src-alpha gl/one]}})

(defn draw
  [^WebGLRenderingContext gl spec]
  (buf/draw-arrays-with-shader gl (sh/inject-normal-matrix spec :model :view :normalMat)))
