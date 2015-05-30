package io.github.daviddenton.fintrospect.renderers.swagger1dot1

import argo.jdom.JsonNode
import com.twitter.finagle.http.path.Path
import io.github.daviddenton.fintrospect.Route
import io.github.daviddenton.fintrospect.parameters.{Parameter, Requirement}
import io.github.daviddenton.fintrospect.renderers.DescriptionRenderer
import io.github.daviddenton.fintrospect.util.ArgoUtil._
import io.github.daviddenton.fintrospect.util.JsonResponseBuilder._
import org.jboss.netty.handler.codec.http.HttpResponse

import scala.collection.JavaConversions._

/**
 * DescriptionRenderer that provides simple Swagger v1.1 support in Argo JSON format
 */
class Swagger1dot1DescriptionRenderer extends DescriptionRenderer {

  private def render(requirementAndParameter: (Requirement, Parameter[_])): JsonNode = obj(
    "name" -> string(requirementAndParameter._2.name),
    "description" -> requirementAndParameter._2.description.map(string).getOrElse(nullNode()),
    "paramType" -> string(requirementAndParameter._2.where.toString),
    "required" -> boolean(requirementAndParameter._1.required),
    "dataType" -> string(requirementAndParameter._2.paramType.name)
  )

  private def render(route: Route): Field = route.method.getName.toLowerCase -> obj(
    "httpMethod" -> string(route.method.getName),
    "nickname" -> string(route.describedRoute.summary),
    "notes" -> string(route.describedRoute.summary),
    "produces" -> array(route.describedRoute.produces.map(m => string(m.value))),
    "consumes" -> array(route.describedRoute.consumes.map(m => string(m.value))),
    "parameters" -> array(route.allParams.map(render)),
    "errorResponses" -> array(route.describedRoute.responses
      .filter(_.status.getCode > 399)
      .map(resp => obj("code" -> number(resp.status.getCode), "reason" -> string(resp.description))).toSeq)
  )

  def apply(basePath: Path, routes: Seq[Route]): HttpResponse = {
    val api = routes
      .groupBy(_.describeFor(basePath))
      .map { case (path, routesForPath) => obj("path" -> string(path), "operations" -> array(routesForPath.map(render(_)._2))) }

    Ok(obj("swaggerVersion" -> string("1.1"), "resourcePath" -> string("/"), "apis" -> array(asJavaIterable(api))))
  }
}
