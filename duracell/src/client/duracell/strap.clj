(ns duracell.strap)

(defn jetty-init []
  (.put (System/getProperties) "org.eclipse.jetty.servlet.Default.useFileMappedBuffer" "false")
  )
