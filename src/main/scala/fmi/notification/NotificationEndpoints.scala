package fmi.notification

import fmi.court.CourtId
import fmi.user.UserId
import fmi.{
  AuthenticationError,
  ConflictDescription,
  ResourceNotFound,
  TennisAppEndpoints,
  NotificationDeletionError,
  NotificationStatusUpdateError
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

  val retrieveNotificationsForCourt
    : Endpoint[String, CourtId, ResourceNotFound | AuthenticationError, List[Notification], Any] =
    notificationsBaseEndpoint.secure
      .in("court")
      .in(path[CourtId].name("court-id"))
      .out(jsonBody[List[Notification]])
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

  val deleteNotificationEndpoint
    : Endpoint[String, NotificationId, AuthenticationError | NotificationDeletionError, Unit, Any] =
    notificationsBaseEndpoint.secure
      .in(path[NotificationId].name("notification-id"))
      .out(statusCode(NoContent))
      .errorOutVariantPrepend[AuthenticationError | NotificationDeletionError](
        oneOfVariant(
          statusCode(BadRequest).and(jsonBody[NotificationDeletionError].description("No such notification was found"))
        )
      )
      .delete

  val retrieveNotificationsForUser
    : Endpoint[String, UserId, ResourceNotFound | AuthenticationError, List[Notification], Any] =
    notificationsBaseEndpoint.secure
      .in("user")
      .in(path[UserId].name("user-id"))
      .out(jsonBody[List[Notification]])
      .errorOutVariantPrepend[AuthenticationError | ResourceNotFound](
        oneOfVariant(statusCode(NotFound).and(jsonBody[ResourceNotFound]))
      )
      .get

  val changeNotificationStatus: Endpoint[
    String,
    NotificationStatusChangeForm,
    AuthenticationError | NotificationStatusUpdateError,
    Notification,
    Any
  ] =
    notificationsBaseEndpoint.secure
      .in(jsonBody[NotificationStatusChangeForm])
      .out(jsonBody[Notification])
      .errorOutVariantPrepend[AuthenticationError | NotificationStatusUpdateError](
        oneOfVariant(
          statusCode(BadRequest)
            .and(jsonBody[NotificationStatusUpdateError].description("Notification status could not be changed."))
        )
      )
      .put
