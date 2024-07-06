package fmi.notification

import cats.effect.IO
import cats.syntax.all.*
import fmi.user.authentication.AuthenticationService
import fmi.{ConflictDescription, ReservationDeletionError, ReservationStatusUpdateError, ResourceNotFound}
import sttp.tapir.server.ServerEndpoint

class NotificationController (
  notificationService: NotificationService
                             )(
  authenticationService: AuthenticationService
  ):
  import authenticationService.authenticate

  private val getNotification = NotificationEndpoints.retrieveNotification
    .authenticate()
    .serverLogic { user =>
      notificationId =>
        println(s"Received request to get notification with id: $notificationId")
        notificationService
          .getNotificationById(notificationId)
          .map {
            case Some(notification) =>
              println(s"Found notification: ${notification.toString}")
              Right(notification)
            case None =>
              println(s"Notification with id $notificationId was not found")
              Left(ResourceNotFound(s"Notification $notificationId was not found"))
          }
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    getNotification,
  )
    