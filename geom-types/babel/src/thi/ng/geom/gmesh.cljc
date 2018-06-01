(ns thi.ng.geom.gmesh
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]
   [thi.ng.geom.core.matrix :refer [M44]]
   [thi.ng.geom.types]
   [thi.ng.geom.types.utils :as tu]
   [thi.ng.dstruct.core :as d]
   [thi.ng.math.core :as m :refer [*eps*]]
   [clojure.core.reducers :as r]
   [clojure.set :as set]))

(defn add-face
  [{:keys [vertices edges faces] :as mesh} verts]
  (let [f (mapv #(get (find vertices %) 0 %) verts)]
    (if (and (nil? (get faces f))
             (= (count f) (count (set f))))
      (let [vertices (->> (d/wrap-seq f [(peek f)] [(first f)])
                          (d/successive-nth 3)
                          (reduce
                           (fn [acc [p c n]]
                             (d/index-kv acc c {:next n :prev p :f f}))
                           vertices))
            edges (->> (conj f (first f))
                       (d/successive-nth 2)
                       (reduce
                        (fn [acc pair] (d/index-kv acc (set pair) f))
                        edges))]
        (assoc mesh
          :vertices vertices
          :edges edges
          :faces (conj faces f)))
      mesh)))

(defn vertices-planar?
  [[a b c :as verts]]
  (or (< (count verts) 4)
      (let [n (gu/ortho-normal a b c)]
        (every? #(m/delta= n (gu/ortho-normal %))
                (d/successive-nth 3 (conj (rest verts) a))))))

(defn face-neighbors-shared-edges
  [{:keys [edges]} f]
  (->> (conj f (first f))
       (d/successive-nth 2)
       (reduce
        (fn [acc pair] (into acc (-> pair set edges (disj f))))
        [])))

(defn vertex-neighbors*
  [{vertices :vertices} v]
  (set/union
   (d/value-set :next vertices v)
   (d/value-set :prev vertices v)))

(defn vertex-valence*
  [mesh v] (inc (count (get (:vertices mesh) v))))

(defn vertex-faces*
  [mesh v] (d/value-set :f (:vertices mesh) v))

(defn remove-vertex*
  [mesh v]
  (if (find (:vertices mesh) v)
    (reduce g/remove-face mesh (vertex-faces* mesh v))
    mesh))

(defn replace-vertex*
  ([mesh v v2]
     (let [vfaces (vertex-faces* mesh v)]
       (-> (reduce g/remove-face mesh vfaces)
           (replace-vertex* v v2 vfaces))))
  ([mesh v v2 faces]
     (reduce #(add-face % (replace {v v2} %2)) mesh faces)))

(defn merge-vertices*
  [mesh a b]
  (if ((vertex-neighbors* mesh a) b)
    (let [fa (vertex-faces* mesh a) fb (vertex-faces* mesh b)
          ab-isec (set/intersection fa fb)
          a-xor (set/difference fa ab-isec)
          b-xor (set/difference fb ab-isec)
          mp (g/mix a b)]
      (-> (reduce g/remove-face mesh (set/union ab-isec a-xor b-xor))
          (replace-vertex* a mp a-xor)
          (replace-vertex* b mp b-xor)))
    mesh))

(defn gmesh
  "Builds a new 3d mesh data structure and (optionally) populates it with
  the given items (a seq of existing meshes and/or faces). Faces are defined
  as vectors of their vertices."
  [] (thi.ng.geom.types.GMesh. {} #{} {} {} {} #{} {}))
(defn lathe-mesh
  [points res phi rot-fn & [face-fn]]
  (let [strips (mapv
                (fn [i]
                  (let [theta (* i phi)]
                    (mapv #(let [p (rot-fn % theta)]
                             (if (m/delta= p % *eps*)
                               % p))
                          points)))
                (butlast (m/norm-range res)))
        strips (if (m/delta= phi m/TWO_PI)
                 (conj strips (first strips))
                 strips)
        make-face (fn [[a1 a2] [b1 b2]]
                    (let [f (cond
                             (< (count (hash-set a1 a2 b1 b2)) 3) nil
                             (= a1 b1) [b1 b2 a2]
                             (= a2 b2) [b1 a2 a1]
                             :default [b1 b2 a2 a1])]
                      (if (and f face-fn) (face-fn f) [f])))]
    (->> (d/successive-nth 2 strips)
         (mapcat ;; TODO transduce
          (fn [[sa sb]]
            (mapcat make-face
                 (d/successive-nth 2 sa)
                 (d/successive-nth 2 sb))))
         (tu/into-mesh (gmesh) add-face))))
(defn saddle
  [s]
  (let [sv (vec3 s)]
    (reduce
     (fn [m [p flags]]
       (tu/into-mesh m add-face (g/as-mesh (thi.ng.geom.types.AABB p s) {:flags flags})))
     (gmesh)
     [[(vec3) :ewsfb]
      [(vec3 0 s 0) :wfb]
      [(vec3 s s 0) :ensfb]
      [(vec3 0 (* s 2) 0) :ewnfb]])))



(extend-type thi.ng.geom.types.GMesh
g/PArea
(area
 [_]
 (transduce
  (comp
   (mapcat gu/tessellate-with-first)
   (map #(->> % (apply gu/tri-area3) m/abs)))
  + (:faces _)))


g/PBounds
(bounds [_] (tu/bounding-box (keys (:vertices _))))
(width [_] (gu/axis-range 0 (keys (:vertices _))))
(height [_] (gu/axis-range 1 (keys (:vertices _))))
(depth [_] (gu/axis-range 2 (keys (:vertices _))))
g/PBoundingSphere
(bounding-sphere
 [_] (tu/bounding-sphere (g/centroid _) (g/vertices _)))
g/PCenter
(center
 ([_] (g/center _ (vec3)))
 ([_ o] (g/transform _ (g/translate M44 (g/- o (g/centroid _))))))
(centroid
 [_] (gu/centroid (keys (:vertices _))))
g/PFlip
(flip [_] (tu/map-mesh (fn [f] [(vec (rseq f))]) _))
g/PGraph
(connected-components
 [_] [_]) ;; TODO
(vertex-neighbors
 [_ v] (vertex-neighbors* _ v))
(vertex-valence
 [_ v] (vertex-valence* _ v))
(remove-vertex
 [_ v] (remove-vertex* _ v))
(replace-vertex
 [_ v v2] (replace-vertex* _ v v2))
(merge-vertices
 [_ a b] (merge-vertices* _ a b))
g/PVertexAccess
(vertices
 [_] (keys (:vertices _)))
g/PEdgeAccess
(edges
 [_] (keys (:edges _)))
g/PFaceAccess
(faces
 [_] (:faces _))
(add-face
 [_ f] (add-face _ f))
(vertex-faces
 [_ v] (vertex-faces* _ v))
(remove-face
 [{:keys [vertices edges faces fnormals vnormals] :as _} f]
 (if (get faces f)
   (loop [verts vertices
          vnorms vnormals
          edges edges
          fedges (d/successive-nth 2 (conj f (first f)))]
     (if fedges
       (let [[a b] (first fedges)
             e #{a b}
             efaces (disj (get edges e) f)
             edges (if (seq efaces)
                     (assoc edges e efaces)
                     (dissoc edges e))
             ve (filter #(not= (:f %) f) (get verts a))]
         (if (seq ve)
           (recur (assoc verts a (into #{} ve)) vnorms edges (next fedges))
           (recur (dissoc verts a) (dissoc vnorms a) edges (next fedges))))
       (assoc _
         :vertices verts
         :vnormals vnorms
         :edges edges
         :faces (disj faces f)
         :fnormals (dissoc fnormals f))))
   _))
g/PNormalAccess
(face-normals
 [_ force?]
 (if (seq (:fnormals _))
   (:fnormals _)
   (if force? (:fnormals (g/compute-face-normals _)))))
(face-normal
 [_ f] ((:fnormals _) f))
(vertex-normals
 [_ force?]
 (if (seq (:vnormals _))
   (:vnormals _)
   (if force? (:vnormals (g/compute-vertex-normals _)))))
(vertex-normal
 [_ v] ((:vnormals _) v))
(compute-face-normals
 [_]
 (loop [norms (transient #{}), fnorms (transient {}), faces (:faces _)]
   (if faces
     (let [f (first faces)
           [norms n] (d/index! norms (apply gu/ortho-normal f))]
       (recur norms (assoc! fnorms f n) (next faces)))
     (assoc _
            :normals  (persistent! norms)
            :fnormals (persistent! fnorms)))))
(compute-vertex-normals
 [_]
 (let [{:keys [vertices normals fnormals] :as this} (if (seq (:fnormals _)) _ (g/compute-face-normals _))
       ntx (map #(get fnormals %))]
   (loop [norms (transient normals), vnorms (transient (hash-map)), verts (keys vertices)]
     (if verts
       (let [v (first verts)
             [norms n] (->> (d/value-set :f vertices v)
                            (transduce ntx g/+ v/V3)
                            (g/normalize)
                            (d/index! norms))]
         (recur norms (assoc! vnorms v n) (next verts)))
       (assoc this
              :normals  (persistent! norms)
              :vnormals (persistent! vnorms))))))
g/PGeomContainer
(into
 [_ faces] (tu/into-mesh _ add-face faces))
g/PClear
(clear*
 [_] (gmesh))
g/PMeshConvert
(as-mesh
 ([_] _)
 ([_ opts] (g/into (:mesh opts) (:faces _))))
g/PTessellate
(tessellate
 ([_]      (g/tessellate _ {}))
 ([_ opts] (tu/map-mesh (or (:fn opts) gu/tessellate-with-first) _)))
g/PScale
(scale
 ([_ s]
    (tu/transform-mesh _ add-face #(g/* % s)))
 ([_ sx sy sz]
    (tu/transform-mesh _ add-face #(g/* % sx sy sz))))
(scale-size
 [_ s]
 (let [c (g/centroid _)]
   (tu/transform-mesh _ add-face #(g/madd (g/- % c) s c))))
g/PTranslate
(translate
 [_ t] (tu/transform-mesh _ add-face #(g/+ % t)))
g/PTransform
(transform
 [_ tx]
 (tu/transform-mesh _ add-face tx))
g/PVolume
(volume
 [_] (gu/total-volume (:faces _)))
)
