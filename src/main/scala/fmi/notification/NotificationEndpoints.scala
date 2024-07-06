package fmi.notification

import fmi.court.CourtId
import fmi.{
  AuthenticationError,
  ConflictDescription,
  ReservationDeletionError,
  ReservationStatusUpdateError,
  ResourceNotFound,
  TennisAppEndpoints
}
import sttp.model.StatusCode.{BadRequest, Conflict, NoContent, NotFound}
import sttp.tapir.{oneOfVariant, statusCode, *}
import sttp.tapir.json.circe.*

object NotificationEndpoints:
  import TennisAppEndpoints.*

  private val notificationsBaseEndpoint = v1BaseEndpoint.in("notifications")

  val retrieveNotification
  : Endpoint[String, NotificationId, ResourceNotFound | AuthenticationError, Notification, Any] =
    notificationsBaseEndpoint.secure
      .in(path[NotificationId].name("notification-id"))
      .out(jsonBody[Notification])
      .errorOutVariantPrepend[AuthenticationError | ResourceNotFound](
        oneOfVariant(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
      )
      .get

  val createNotification
  : Endpoint[String, NotificationForm, AuthenticationError | ConflictDescription, Notification, Any] =
    notificationsBaseEndpoint.secure
      .in(jsonBody[NotificationForm])
      .out(jsonBody[Notification])
      .errorOutVariantPrepend[AuthenticationError | ConflictDescription](
        oneOfVariant(statusCode(Conflict).and(jsonBody[ConflictDescription]))
      )
      .post


 
