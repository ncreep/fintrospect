package io.fintrospect

import com.twitter.finagle.Service
import com.twitter.finagle.http.path.Path
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.util.Future
import io.fintrospect.Module.ServiceBinding
import io.fintrospect.formats.{AbstractResponseBuilder, Argo}
import io.fintrospect.renderers.JsonErrorResponseRenderer

object Module {

  type ServiceBinding = PartialFunction[(Method, Path), Service[Request, Response]]

  /**
    * Combines many modules
    */
  def combine(modules: Module*): Module = new Module {
    override protected[fintrospect] def serviceBinding: ServiceBinding = modules.map(_.serviceBinding).reduce(_.orElse(_))
  }

  /**
    * Convert a ServiceBinding to a Finagle Service
    */
  def toService(binding: ServiceBinding, responseBuilder: AbstractResponseBuilder[_] = Argo.ResponseBuilder): Service[Request, Response] = {
    val notFoundPf: ServiceBinding = {
      case _ => Service.mk { _ => Future(JsonErrorResponseRenderer.notFound()) }
    }

    Service.mk { request => (binding orElse notFoundPf) ((request.method, Path(request.path)))(request) }
  }
}

trait Module {

  /**
    * Finaliser for the module builder to combine itself with another module into a Partial Function which matches incoming requests.
    */
  def combine(that: Module): Module = Module.combine(this, that)

  /**
    * Finaliser for the module builder to convert itself to a Finagle Service. Use this function when there is only one module.
    */
  def toService: Service[Request, Response] = Module.toService(serviceBinding)

  protected[fintrospect] def serviceBinding: ServiceBinding
}
