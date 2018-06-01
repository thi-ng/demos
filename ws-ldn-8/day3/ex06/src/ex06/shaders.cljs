(ns ex06.shaders
  (:require
   [thi.ng.geom.matrix :as mat]
   [thi.ng.geom.gl.fx :as fx]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.color :as col]
   [thi.ng.glsl.vertex :as vertex]
   [thi.ng.dstruct.core :as d]))

(def threshold-shader-spec
  (d/merge-deep
   fx/shader-spec
   {:fs (->> "void main() {
    vec3 rgb = texture2D(tex, vUV).rgb;
    float c = threshold(rgb, thresh1, thresh2);
    gl_FragColor = vec4(c, c, c, 1.0);
  }"
             (glsl/glsl-spec-plain [col/threshold])
             (glsl/assemble))
    :uniforms {:thresh1 [:float 0.4]
               :thresh2 [:float 0.401]
               :time    [:float 0]}}))

(def hueshift-shader-spec
  (d/merge-deep
   fx/shader-spec
   {:fs (->> "void main() {
    vec3 rgb = texture2D(tex, vUV).rgb;
    gl_FragColor = vec4(rotateHueRGB(rgb, time), 1.0);
  }"
             (glsl/glsl-spec-plain [col/rotate-hue-rgb])
             (glsl/assemble))
    :uniforms {:time [:float 0]}}))

(def twirl-shader-spec
  (d/merge-deep
   fx/shader-spec
   {:fs (->> "void main() {
    vec2 uv = vUV - vec2(0.5);
    float theta = 10.0 * sin(time) * length(uv);
    float c = cos(theta);
    float s = sin(theta);
    uv = vec2(uv.x * c - uv.y * s, uv.x * s + uv.y * c) + vec2(0.5);
    vec3 rgb = texture2D(tex, uv).rgb;
    gl_FragColor = vec4(rgb, 1.0);
  }"
             (glsl/glsl-spec-plain [col/threshold])
             (glsl/assemble))
    :uniforms {:time [:float 0.0]}}))

(def pixelate-shader-spec
  (d/merge-deep
   fx/shader-spec
   {:fs (->> "void main() {
    vec2 uv = floor(vUV / (1.0 / size) + 0.5) / size;
    gl_FragColor = vec4(texture2D(tex, uv).rgb, 1.0);
  }"
             (glsl/glsl-spec-plain [])
             (glsl/assemble))
    :uniforms {:time [:float 0.0]
               :size [:float 32]}}))

(def tile-shader-spec
  (d/merge-deep
   fx/shader-spec
   {:fs (->> "void main() {
    gl_FragColor = vec4(texture2D(tex, fract(vUV * 32.0 * (sin(time) * 0.4 + 0.5))).rgb, 1.0);
  }"
             (glsl/glsl-spec-plain [])
             (glsl/assemble))
    :uniforms {:time [:float 0.0]
               :size [:float 32]}}))

(def cube-shader-spec
  {:vs "void main() {
    vUV = uv;
    gl_Position = proj * view * model * vec4(position, 1.0);
    }"
   :fs "void main() {
    gl_FragColor = texture2D(tex, vUV);
    }"
   :uniforms {:model    [:mat4 mat/M44]
              :view     :mat4
              :proj     :mat4
              :tex      [:sampler2D 0]}
   :attribs  {:position :vec3
              :uv       :vec2}
   :varying  {:vUV      :vec2}
   :state    {:depth-test true
              ;;:blend      true
              ;;:blend-fn   [glc/src-alpha glc/one]
              }})
