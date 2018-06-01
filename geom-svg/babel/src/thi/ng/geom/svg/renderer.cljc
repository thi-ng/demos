(ns thi.ng.geom.svg.renderer
  (:require
   [thi.ng.geom.core :as g]
   [thi.ng.geom.svg.core :as svg]
   [thi.ng.geom.svg.shaders :as shader]
   [thi.ng.geom.core.utils :as gu]
   [thi.ng.geom.core.vector :refer [vec3 V3Z]]
   [thi.ng.geom.core.matrix :as mat :refer [M44]]))

(defn project-face
  [mvp vtx points]
  (mapv #(mat/project-point-z % mvp vtx) points))

(defn project-faces
  [mvp vtx faces]
  (map
   (fn [f]
     (let [f' (project-face mvp vtx f)
           n' (gu/ortho-normal f')]
       [f f' n']))
   faces))

(defn cull-backfaces
  [norm-fn faces]
  (filter (fn [f] (neg? (g/dot (norm-fn f) V3Z))) faces))

(defn z-map-faces
  [faces]
  (mapv
   (fn [[f f' n']] [(:z (gu/centroid f')) f f' n'])
   faces))

(defn z-sort-faces
  [z-fn faces]
  (reverse (sort-by z-fn faces)))

(defn mesh
  [mesh mvp screen shader]
  (let [faces (project-faces mvp screen (g/faces mesh))
        faces (->> (if (shader/solid? shader)
                     (cull-backfaces peek faces)
                     faces)
                   (z-map-faces)
                   (z-sort-faces first))]
    (svg/group
     (shader/uniforms shader)
     (if shader
       (map (fn [[z f f']] (svg/polygon f' (shader/shade-facet shader f f' z))) faces)
       (map (fn [f] (svg/polygon (f 2) nil)) faces)))))
