package fmi

import io.circe.Codec
import sttp.model.StatusCode.{Forbidden, Unauthorized}
import sttp.tapir.*
import sttp.tapir.json.circe.*

sealed trait HttpError
sealed trait AuthenticationError extends HttpError 
case class InsufficientPermissionError(message: String) extends AuthenticationError derives Codec, Schema
case class UnauthorizedAccess(message: String) extends AuthenticationError derives Codec, Schema
case class ForbiddenResource(message: String) extends AuthenticationError derives Codec, Schema
case class ResourceNotFound(message: String) extends HttpError derives Codec, Schema
case class OperationNotAllowed(message: String) extends HttpError derives Codec, Schema
case class BadRequestDescription[E](error: E) extends HttpError
case class ConflictDescription(message: String) extends HttpError derives Codec, Schema
case class ReservationDeletionError(message: String) extends HttpError derives Codec, Schema
case class ReservationStatusUpdateError(message: String) extends HttpError derives Codec, Schema
case class ReservationCreationError(message: String) extends HttpError derives Codec, Schema
case class NotificationDeletionError(message: String) extends HttpError derives Codec, Schema
case class NotificationStatusUpdateError(message: String) extends HttpError derives Codec, Schema

object BadRequestDescription:
  given [E : Codec]: Codec[BadRequestDescription[E]] = Codec.derived
  given [E : Schema]: Schema[BadRequestDescription[E]] = Schema.derived

object TennisAppEndpoints:
  private val baseEndpoint: PublicEndpoint[Unit, Unit, Unit, Any] = endpoint
  val v1BaseEndpoint: Endpoint[Unit, Unit, Unit, Unit, Any] = baseEndpoint.in("v1")

  extension [I, O, R](endpoint: PublicEndpoint[I, Unit, O, R])
    def secure: Endpoint[String, I, AuthenticationError, O, R] =
      endpoint
        .securityIn(cookie[String]("loggedUser"))
        .errorOut(
          oneOf[AuthenticationError](
            oneOfVariant(statusCode(Unauthorized).and(jsonBody[UnauthorizedAccess].description("unauthorized access"))),
            oneOfVariant(statusCode(Forbidden).and(jsonBody[ForbiddenResource].description("resource forbidden")))
          )
        )
