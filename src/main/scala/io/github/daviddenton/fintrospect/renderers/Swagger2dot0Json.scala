package io.github.daviddenton.fintrospect.renderers

import argo.jdom.{JsonNode, JsonRootNode}
import io.github.daviddenton.fintrospect._
import io.github.daviddenton.fintrospect.parameters.{Body, Parameter, Requirement}
import io.github.daviddenton.fintrospect.util.ArgoUtil._

class Swagger2dot0Json private(apiInfo: ApiInfo) extends Renderer {

  private val schemaGenerator = new JsonToJsonSchema()

  private case class FieldAndDefinitions(field: Field, definitions: List[Field])

  private case class FieldsAndDefinitions(fields: List[Field] = Nil, definitions: List[Field] = Nil) {
    def add(newField: Field, newDefinitions: List[Field]) = FieldsAndDefinitions(newField :: fields, newDefinitions ++ definitions)

    def add(fieldAndDefinitions: FieldAndDefinitions) = FieldsAndDefinitions(fieldAndDefinitions.field :: fields, fieldAndDefinitions.definitions ++ definitions)
  }

  private def render(requirementAndParameter: (Requirement, Parameter[_])): JsonNode = obj(
    "in" -> string(requirementAndParameter._2.where.toString),
    "name" -> string(requirementAndParameter._2.name),
    "description" -> requirementAndParameter._2.description.map(string).getOrElse(nullNode()),
    "required" -> boolean(requirementAndParameter._1.required),
    "type" -> string(requirementAndParameter._2.paramType.name)
  )

  private def render(body: Body, schema: Schema): JsonNode = obj(
    "in" -> string(body.where.toString),
    "name" -> string(body.name),
    "description" -> body.description.map(string).getOrElse(nullNode()),
    "required" -> boolean(true),
    "schema" -> schema.node
  )

  private def renderRoute(mr: ModuleRoute): FieldAndDefinitions = {
    val FieldsAndDefinitions(responses, responseDefinitions) = renderResponses(mr.route.description.responses)

    val bodySchema = mr.route.description.body.map(b => schemaGenerator.toSchema(b.example))
    val bodyParameters = bodySchema.toList.flatMap(s => Seq(render(mr.route.description.body.get, s)))

    val route = mr.route.method.getName.toLowerCase -> obj(
      "tags" -> array(string(mr.basePath.toString)),
      "summary" -> mr.route.description.summary.map(string).getOrElse(nullNode()),
      "produces" -> array(mr.route.description.produces.map(m => string(m.value))),
      "consumes" -> array(mr.route.description.consumes.map(m => string(m.value))),
      "parameters" -> array(mr.route.allParams.map(render) ++ bodyParameters),
      "responses" -> obj(responses),
      "security" -> array(obj(Seq[Security]().map(_.toPathSecurity)))
    )
    FieldAndDefinitions(route, responseDefinitions ++ bodySchema.toList.flatMap(_.definitions))
  }

  private def renderResponses(responses: List[ResponseWithExample]): FieldsAndDefinitions = {
    responses.foldLeft(FieldsAndDefinitions()) {
      case (memo, nextResp) =>
        val newSchema = Option(nextResp.example).map(schemaGenerator.toSchema).getOrElse(Schema(nullNode(), Nil))
        val newField = nextResp.status.getCode.toString -> obj("description" -> string(nextResp.description), "schema" -> newSchema.node)
        memo.add(newField, newSchema.definitions)
    }
  }

  private def renderApiInfo(apiInfo: ApiInfo): JsonNode = {
    obj("title" -> string(apiInfo.title), "version" -> string(apiInfo.version), "description" -> string(apiInfo.description.getOrElse("")))
  }

  def apply(moduleRoutes: Seq[ModuleRoute]): JsonRootNode = {
    val pathsAndDefinitions = moduleRoutes
      .groupBy(_.toString)
      .foldLeft(FieldsAndDefinitions()) {
      case (memo, (path, routesForThisPath)) =>
        val routeFieldsAndDefinitions = routesForThisPath.foldLeft(FieldsAndDefinitions()) {
          case (memoFields, mr) => memoFields.add(renderRoute(mr))
        }
        memo.add(path -> obj(routeFieldsAndDefinitions.fields), routeFieldsAndDefinitions.definitions)
    }
    obj(
      "swagger" -> string("2.0"),
      "info" -> renderApiInfo(apiInfo),
      "basePath" -> string("/"),
      "paths" -> obj(pathsAndDefinitions.fields),
      "definitions" -> obj(pathsAndDefinitions.definitions)
    )
  }
}

/**
 * Renderer that provides Swagger v2.0 support
 */
object Swagger2dot0Json {
  def apply(apiInfo: ApiInfo): Renderer = new Swagger2dot0Json(apiInfo)
}