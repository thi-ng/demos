(defproject thi.ng/geom "0.0.921"
  :description  "thi.ng geometry kit - meta project spec including all modules"
  :url          "https://github.com/thi-ng/geom"
  :license      {:name "Apache Software License"
                 :url  "http://www.apache.org/licenses/LICENSE-2.0"
                 :distribution :repo}
  :scm          {:name "git"
                 :url  "https://github.com/thi-ng/geom"}

  :dependencies [[thi.ng/geom-core "0.0.921"]
                 [thi.ng/geom-types "0.0.921"]
                 [thi.ng/geom-meshops "0.0.921"]
                 [thi.ng/geom-physics "0.0.921"]
                 [thi.ng/geom-svg "0.0.921"]
                 [thi.ng/geom-viz "0.0.921"]
                 [thi.ng/geom-voxel "0.0.921"]
                 [thi.ng/geom-webgl "0.0.921"]]

  :pom-addition [:developers [:developer
                              [:name "Karsten Schmidt"]
                              [:url "http://postspectacular.com"]
                              [:timezone "0"]]])
