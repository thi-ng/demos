(ns thi.ng.geom.webgl.shaders
  (:require-macros
    [cljs-log.core :refer [info warn]])
  (:require
    [thi.ng.geom.core :as g]
    [thi.ng.geom.core.matrix]
    [thi.ng.geom.webgl.core :as gl]
    [thi.ng.geom.webgl.utils :as glu]
    [thi.ng.typedarrays.core :as arrays]
    [thi.ng.color.core :as col]
    [thi.ng.xerror.core :as err]
    [clojure.string :as str]))

  (def uniform-types
    {:float       ["1fv" arrays/float32]
     :int         ["1iv" arrays/int32]
     :vec2        ["2fv" arrays/float32]
     :vec3        ["3fv" arrays/float32]
     :vec4        ["4fv" arrays/float32]
     :ivec2       ["2iv" arrays/int32]
     :ivec3       ["3iv" arrays/int32]
     :ivec4       ["4iv" arrays/int32]
     :bool        ["1iv" arrays/int32]
     :bvec2       ["2iv" arrays/int32]
     :bvec3       ["3iv" arrays/int32]
     :bvec4       ["4iv" arrays/int32]
     :mat2        ["Matrix2fv" arrays/float32]
     :mat3        ["Matrix3fv" arrays/float32]
     :mat4        ["Matrix4fv" arrays/float32]
     :sampler2D   ["1iv" arrays/int32]
     :samplerCube ["1iv" arrays/int32]})
  
  (defn init-shader-uniforms
    [^WebGLRenderingContext gl prog uniforms]
    (reduce
     (fn [umap [id type]]
       (let [loc (.getUniformLocation gl prog (name id))
             [type default transpose?] (if (sequential? type) type [type])
             transpose? (boolean transpose?)
             [u-type u-cast] (uniform-types type)
             setter (aget gl (str "uniform" u-type))]
         ;; TODO add check if valid uniform
         (assoc umap id
                {:type type
                 :default default
                 :setter (cond
                           (#{:mat2 :mat3 :mat4} type)
                           (fn [x]
                             (.call setter gl loc transpose?
                                    (if (arrays/typed-array? x)
                                      x (u-cast x))))
                           (= :vec3 type)
                           (fn [x]
                             (if (number? x)
                               (.call setter gl loc (-> x col/int24 col/as-rgba deref (subvec 0 3) u-cast))
                               (.call setter gl loc (if (arrays/typed-array? x) x (u-cast x)))))
                           :else
                           (fn [x]
                             (.call setter gl loc
                                    (if (arrays/typed-array? x)
                                      x (u-cast (if (not (sequential? x)) [x] x))))))
                 :loc loc})))
     {} uniforms))
  
  (defn set-uniform
    [shader id val]
    (if-let [u-spec (get-in shader [:uniforms id])]
      ((:setter u-spec) val)
      (warn "Unknown shader uniform: " id)))

  (defn init-shader-attribs
    [^WebGLRenderingContext gl prog attribs]
    (reduce
     (fn [amap id]
       (assoc amap id (.getAttribLocation gl prog (name id))))
     {} attribs))
  
  (defn set-attribute
    [^WebGLRenderingContext gl shader id {:keys [buffer stride size type normalized? offset]}]
    (if-let [loc (get-in shader [:attribs id])]
      (doto gl
        (.bindBuffer gl/array-buffer buffer)
        (.enableVertexAttribArray loc)
        (.vertexAttribPointer
         loc
         size
         (or type gl/float)
         (boolean normalized?)
         (or stride 0)
         (or offset 0)))
      (err/illegal-arg! (str "Unknown shader attribute: " id))))
  
  (defn disable-attribute
    [^WebGLRenderingContext gl shader id]
    (if-let [loc (get-in shader [:attribs id])]
      (do (.disableVertexAttribArray gl loc) gl)
      (err/illegal-arg! (str "Unknown shader attribute: " id))))

    (def header-prefix
      "
  #ifdef GL_FRAGMENT_PRECISION_HIGH
  precision highp int;
  precision highp float;
  #else
  precision mediump int;
  precision mediump float;
  #endif
  #ifndef PI
  #define PI      3.141592653589793
  #endif
  #ifndef TWO_PI
  #define TWO_PI  6.283185307179586
  #endif
  #ifndef HALF_PI
  #define HALF_PI 1.570796326794896
  #endif
  #ifndef RAD
  #define RAD     0.008726646259972
  #endif
  ")
  (defn compile-glsl-vars
    [qualifier coll]
    (->> coll
         (map
          (fn [[id type]]
            (str qualifier " "
                 (name (if (sequential? type) (first type) type)) " "
                 (name id) ";\n")))
         (apply str)))
  
  (defn parse-and-throw-error
    [^WebGLRenderingContext gl shader src]
    (let [src-lines (vec (str/split-lines src))
          errors (->> shader
                      (.getShaderInfoLog gl)
                      (str/split-lines)
                      (map
                       (fn [line]
                         (let [[_ ln msg] (re-find #"ERROR: \d+:(\d+): (.*)" line)]
                           (when ln
                             (str "line " ln ": " msg "\n"
                                  (src-lines (dec (js/parseInt ln 10))))))))
                      (filter identity)
                      (str/join "\n"))]
      (.deleteShader gl shader)
      (err/throw! (str "Error compiling shader:\n" errors))))
  
  (defn compile-shader
    [^WebGLRenderingContext gl src type]
    (if-let [shader (.createShader gl type)]
      (do
        (.shaderSource gl shader src)
        (.compileShader gl shader)
        (if (.getShaderParameter gl shader gl/compile-status)
          shader
          (parse-and-throw-error gl shader src)))
      (err/throw! "Can't create shader")))
  
  (defn make-shader-from-spec
    [^WebGLRenderingContext gl {:keys [vs fs uniforms attribs varying prefix state]}]
    (let [u-src  (compile-glsl-vars "uniform" uniforms)
          a-src  (compile-glsl-vars "attribute" attribs)
          v-src  (compile-glsl-vars "varying" varying)
          prefix (str (or prefix header-prefix) u-src v-src)
          vs     (compile-shader gl (str prefix a-src vs) gl/vertex-shader)
          fs     (compile-shader gl (str prefix fs) gl/fragment-shader)
          prog   (.createProgram gl)]
      (doto gl
        (.attachShader prog vs)
        (.attachShader prog fs)
        (.linkProgram prog))
      (if (.getProgramParameter gl prog gl/link-status)
        (let [uniforms (init-shader-uniforms gl prog uniforms)
              attribs (init-shader-attribs gl prog (keys attribs))]
          (doto gl
            (.deleteShader vs)
            (.deleteShader fs))
          {:program  prog
           :uniforms uniforms
           :attribs  attribs
           :varying  varying
           :state    state})
        (err/throw! (str "Shader failed to link:" (.getProgramInfoLog gl prog))))))
  
  (defn make-shader-from-dom
    [^WebGLRenderingContext gl {:keys [vs fs] :as spec}]
    (make-shader-from-spec
     gl (assoc spec
          :vs (glu/get-script-text vs)
          :fs (glu/get-script-text fs))))

  (defn apply-default-uniforms
    [shader uniforms]
    (->> (keys uniforms)
         (apply dissoc (:uniforms shader))
         (transduce
          (filter #(:default (val %)))
          (completing #(set-uniform shader (key %2) (:default (val %2))))
          nil)))
  
  (defn begin-shader
    [^WebGLRenderingContext gl shader uniforms attribs]
    (.useProgram gl (:program shader))
    (apply-default-uniforms shader uniforms)
    (reduce-kv #(set-uniform shader %2 %3) nil uniforms)
    (reduce-kv #(set-attribute gl shader %2 %3) nil attribs))
  
  (defn end-shader
    [^WebGLRenderingContext gl shader]
    (reduce #(disable-attribute gl shader (key %2)) nil (:attribs shader)))

  (defn prepare-render-state
    "Takes a Gl context and shader spec, sets GL render flags stored
    under :state key (only if :state is present)."
    [gl {:keys [state]}]
    (when state
      (if (:depth-test state)
        (gl/enable gl gl/depth-test)
        (gl/disable gl gl/depth-test))
      (if (:blend state)
        (let [[src dest] (or (:blend-fn state) [gl/src-alpha gl/one])]
          (doto gl
            (gl/enable gl/blend)
            (.blendFunc src dest)))
        (gl/disable gl gl/blend)))
    gl)

  (defn compute-normal-matrix
    [m v] (-> v (g/* m) (g/invert) (g/transpose)))
  
  (defn inject-normal-matrix
    [spec model-mat view-mat normal-mat-id]
    (let [model-mat (if (keyword? model-mat)
                      (-> spec :uniforms model-mat)
                      model-mat)
          view-mat (if (keyword? view-mat)
                      (-> spec :uniforms view-mat)
                      view-mat)]
      (assoc-in
       spec [:uniforms normal-mat-id]
       (compute-normal-matrix model-mat view-mat))))
