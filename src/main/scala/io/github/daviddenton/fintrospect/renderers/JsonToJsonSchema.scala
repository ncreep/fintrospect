package io.github.daviddenton.fintrospect.renderers

import argo.jdom.JsonNodeFactories.string
import argo.jdom.JsonNodeType._
import argo.jdom.{JsonField, JsonNode, JsonNodeFactories, JsonRootNode}
import io.github.daviddenton.fintrospect.renderers.JsonToJsonSchema.IllegalSchemaException
import io.github.daviddenton.fintrospect.util.ArgoUtil._

import scala.collection.JavaConversions._

/*
{
  "Pet": {
    "properties": {
      "id": {
        "type": "integer",
        "format": "int64"
      },
      "category": {"$ref": "#/definitions/Category"},
      "name": {
        "type": "string",
        "example": "doggie"
      },
      "photoUrls": {
        "type": "array",
        "xml": {
          "name": "photoUrl",
          "wrapped": true
        },
        "items": {"type": "string"}
      },
      "tags": {
        "type": "array",
        "xml": {
          "name": "tag",
          "wrapped": true
        },
        "items": {"$ref": "#/definitions/Tag"}
      },
      "status": {
        "type": "string",
        "description": "pet status in the store",
        "enum": [
          "available",
          "pending",
          "sold"
        ]
      }
    },
    "xml": {"name": "Pet"}
  }

 */

object JsonToJsonSchema {

  class IllegalSchemaException(message: String) extends Exception(message)

  def toSchema(input: JsonNode): JsonRootNode = {
    input.getType match {
      case NULL => throw new IllegalSchemaException("Cannot use a null value in a schema!")
      case STRING => obj("type" -> string("string"))
      case TRUE => obj("type" -> string("boolean"))
      case FALSE => obj("type" -> string("boolean"))
      case NUMBER => obj("type" -> string("number"))
      case ARRAY => obj("type" -> string("array"), "items" -> input.getElements.to[Seq].headOption.map(toSchema).getOrElse(throw new IllegalSchemaException("Cannot use an empty list for a schema!")))
      case OBJECT => obj("type" -> string("object"), "properties" -> obj(input.getFieldList.to[Seq].map(f => f.getName.getText -> toSchema(f.getValue)): _*))
    }
  }
}

class JsonToJsonSchema(input: JsonNode, idGen: () => String) {

  private var definitions: List[JsonField] = Nil // ewww - mutability! replace with recursion

  private def toSchema(input: JsonNode): JsonRootNode = {
    input.getType match {
      case NULL => throw new IllegalSchemaException("Cannot use a null value in a schema!")
      case STRING => obj("type" -> string("string"))
      case TRUE => obj("type" -> string("boolean"))
      case FALSE => obj("type" -> string("boolean"))
      case NUMBER => obj("type" -> string("number"))
      case ARRAY => obj("type" -> string("array"), "items" -> input.getElements.to[Seq].headOption.map(toSchema).getOrElse(throw new IllegalSchemaException("Cannot use an empty list for a schema!")))
      case OBJECT => objectToSchema(input)
    }
  }

  private def objectToSchema(input: JsonNode): JsonRootNode = {
    val definitionId = idGen()
    definitions = JsonNodeFactories.field(definitionId, obj("type" -> string("object"), "properties" -> obj(input.getFieldList.to[Seq].map(f => f.getName.getText -> toSchema(f.getValue)): _*))) :: definitions
    obj("$ref" -> string(s"#/definitions/$definitionId"))
  }

  def toSchemaAndDefinitions(): (JsonRootNode, List[JsonField]) = {
    (toSchema(input), definitions)
  }
}
