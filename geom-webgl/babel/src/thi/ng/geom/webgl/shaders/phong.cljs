(ns thi.ng.geom.webgl.shaders.phong
  (:require
   [thi.ng.geom.core.matrix :refer [M44]]
   [thi.ng.geom.webgl.buffers :as buf]
   [thi.ng.geom.webgl.shaders :as sh]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.vertex :as vert]
   [thi.ng.glsl.lighting :as light]))

(def shader-spec
  {:vs (glsl/assemble
        (glsl/glsl-spec
         [vert/normal]
         "
void main(){
  vec4 worldPos = model * vec4(position, 1.0);
  vec4 eyePos = view * worldPos;
  vEyePos = eyePos.xyz;
  vNormal = surfaceNormal(normal, normalMat);
  vLightPos = (view * vec4(lightPos, 1.0)).xyz;
  gl_Position = proj * eyePos;
}"))
   :fs (glsl/assemble
        (glsl/glsl-spec
         [light/phong light/blinn-phong]
         "
void main() {
  vec3 L = normalize(vLightPos - vEyePos);
  vec3 E = normalize(-vEyePos);
  vec3 N = normalize(vNormal);

  float NdotL = max(0.0, (dot(N, L) + wrap) / (1.0 + wrap));
  vec3 color = ambientCol + NdotL * diffuseCol;

  float specular = 0.0;
  if (useBlinnPhong) {
    specular = blinnPhong(L, E, N);
  } else {
    specular = phong(L, E, N);
  }
  color += max(pow(specular, shininess), 0.0) * specularCol;
  gl_FragColor = vec4(color, 1.0);
}"))
   :uniforms {:view          :mat4
              :proj          :mat4
              :model         [:mat4 M44]
              :normalMat     :mat4
              :shininess     [:float 32]
              :ambientCol    [:vec3 [0 0 0]]
              :diffuseCol    [:vec3 [0.8 0.8 0.8]]
              :specularCol   [:vec3 [1 1 1]]
              :lightPos      [:vec3 [0 0 0]]
              :useBlinnPhong [:bool true]
              :wrap          [:float 0]}
   :attribs {:position       :vec3
             :normal         :vec3}
   :varying {:vNormal        :vec3
             :vEyePos        :vec3
             :vLightPos      :vec3}
   :state    {:depth-test true}})

(defn draw
  [^WebGLRenderingContext gl spec]
  (buf/draw-arrays-with-shader gl (sh/inject-normal-matrix spec :model :view :normalMat)))
