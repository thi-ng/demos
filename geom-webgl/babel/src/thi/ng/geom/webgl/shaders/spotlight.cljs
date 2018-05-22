(ns thi.ng.geom.webgl.shaders.spotlight
  (:require
   [thi.ng.geom.core.matrix :refer [M44]]
   [thi.ng.glsl.core :as glsl :include-macros true]
   [thi.ng.glsl.lighting :as light]))

(def shader-spec
  {:vs (glsl/minified "
void main(){
  vNormal = normal;
  vWorldPos = model * vec4(position, 1.0);
  gl_Position = proj * view * vWorldPos;
}")
   :fs (glsl/assemble
        (glsl/glsl-spec
         [light/spotlight-attenuation light/spotlight-influence
          light/lambert light/skylight]
         "
vec3 gamma(vec3 color){
  return pow(color, vec3(2.2));
}

void main(){
  vec3 worldNormal = normalize(vNormal);

  vec3 camPos = (view * vWorldPos).xyz;
  vec3 lightPos = (lightView * vWorldPos).xyz;
  vec3 lightPosNormal = normalize(lightPos);
  vec3 lightSurfaceNormal = lightRot * worldNormal;

  vec3 excident = (skylight(worldNormal.y) +
                   lambert(lightSurfaceNormal, -lightPosNormal) *
                   spotlightInfluence(lightPosNormal, coneAngle, 10.0) *
                   spotlightAttenuation(lightPos, 10.0));
  gl_FragColor = vec4(gamma(excident), 1.0);
}"))
   :uniforms {:view :mat4
              :proj :mat4
              :model [:mat4 M44]
              :lightView :mat4
              :lightRot :mat3
              :coneAngle [:float 15]}
   :attribs {:position :vec3
             :normal :vec3}
   :varying {:vNormal :vec3
             :vWorldPos :vec4}})
