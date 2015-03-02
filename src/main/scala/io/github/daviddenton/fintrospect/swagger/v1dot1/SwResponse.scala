package io.github.daviddenton.fintrospect.swagger.v1dot1

import argo.jdom.JsonNode
import argo.jdom.JsonNodeFactories._
import io.github.daviddenton.fintrospect.util.ArgoUtil._

case class SwResponse(private val code: Int, private val description: String) {
   protected[v1dot1] def toJsonPair: (String, JsonNode) = code.toString -> obj("description" -> string(description))
 }
