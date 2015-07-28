package io.fintrospect

import argo.jdom.JsonNode
import io.fintrospect.parameters.{Body, HeaderParameter, QueryParameter}
import io.fintrospect.util.ArgoUtil._
import io.fintrospect.util.HttpRequestResponseUtil.contentFrom
import org.jboss.netty.handler.codec.http.{HttpMethod, HttpResponse, HttpResponseStatus}

import scala.util.Try

/**
 * Encapsulates the specification of an HTTP endpoint, for use by either the
 */
case class RouteSpec private(summary: String,
                             produces: Set[ContentType],
                             consumes: Set[ContentType],
                             body: Option[Body[_]],
                             headerParams: Seq[HeaderParameter[_]],
                             queryParams: Seq[QueryParameter[_]],
                             responses: Seq[ResponseWithExample]) {

  /**
   * Register content types which the route will consume. This is informational only and is NOT currently enforced.
   */
  def consuming(contentTypes: ContentType*): RouteSpec = copy(consumes = produces ++ contentTypes)

  /**
   * Register content types which thus route will produce. This is informational only and NOT currently enforced.
   */
  def producing(contentTypes: ContentType*): RouteSpec = copy(produces = produces ++ contentTypes)

  /**
   * Register a header parameter. Mandatory parameters are checked for each request, and a 400 returned if any are missing.
   */
  def taking(rp: HeaderParameter[_]): RouteSpec = copy(headerParams = rp +: headerParams)

  /**
   * Register a query parameter. Mandatory parameters are checked for each request, and a 400 returned if any are missing.
   */
  def taking(rp: QueryParameter[_]): RouteSpec = copy(queryParams = rp +: queryParams)

  /**
   * Register the expected content of the body.
   */
  def body(bp: Body[_]): RouteSpec = copy(body = Option(bp), consumes = consumes + bp.contentType)

  /**
   * Register a possible response which could be produced by this route, with an example JSON body (used for schema generation).
   */
  def returning(newResponse: ResponseWithExample): RouteSpec = copy(responses = newResponse +: responses)

  /**
   * Register one or more possible responses which could be produced by this route.
   */
  def returning(codes: (HttpResponseStatus, String)*): RouteSpec = copy(responses = responses ++ codes.map(c => ResponseWithExample(c._1, c._2)))

  /**
   * Register an exact possible response which could be produced by this route. Will be used for schema generation if content is JSON.
   */
  def returning(response: HttpResponse): RouteSpec = {
    returning(ResponseWithExample(response.getStatus, response.getStatus.getReasonPhrase, Try(parse(contentFrom(response))).getOrElse(nullNode())))
  }

  /**
   * Register a possible response which could be produced by this route, with an example JSON body (used for schema generation).
   */
  def returning(code: (HttpResponseStatus, String), example: JsonNode): RouteSpec = copy(responses = ResponseWithExample(code._1, code._2, example) +: responses)

  def at(method: HttpMethod) = IncompletePath(this, method)
}

object RouteSpec {
  def apply(summary: String = "<unknown>"): RouteSpec = RouteSpec(summary, Set.empty, Set.empty, None, Nil, Nil, Nil)
}