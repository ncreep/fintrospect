package examples.circe


import com.twitter.finagle.Service
import com.twitter.finagle.http.Method.Get
import com.twitter.finagle.http.Status.Ok
import com.twitter.finagle.http.{Request, Response}
import io.circe.generic.auto._
import io.fintrospect.RouteSpec
import io.fintrospect.formats.json.Circe.JsonFormat.{encode, responseSpec}
import io.fintrospect.formats.json.Circe.ResponseBuilder.implicits.statusToResponseBuilderConfig
import io.fintrospect.parameters.{ParameterSpec, Path, StringParamType}

class EmailList(emails: Emails) {
  private val emailAddress = Path(ParameterSpec[EmailAddress]("address", Option("user email"), StringParamType, EmailAddress, e => e.address))

  private val exampleEmail = Email(EmailAddress("you@github.com"), EmailAddress("wife@github.com"), "when are you going to be home for dinner", 250)

  private def forUser(emailAddress: EmailAddress) = Service.mk[Request, Response] { _ => Ok(encode(emails.forUser(emailAddress))) }

  val route = RouteSpec("list the inbox contents")
    .returning(responseSpec(Ok -> "list of emails for a user", Seq(exampleEmail)))
    .at(Get) / "user" / emailAddress bindTo forUser
}

