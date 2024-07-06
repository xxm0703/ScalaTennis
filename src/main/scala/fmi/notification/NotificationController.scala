package fmi.notification

import cats.effect.IO
import cats.syntax.all.*
import fmi.user.UserRole
import fmi.user.authentication.AuthenticationService
import fmi.{ConflictDescription, NotificationDeletionError, ReservationStatusUpdateError, ResourceNotFound}
import sttp.tapir.server.ServerEndpoint
import fmi.user.UserRole.Admin

class NotificationController(
  notificationService: NotificationService
)(
  authenticationService: AuthenticationService
):
  import authenticationService.authenticate

  private val getNotification = NotificationEndpoints.retrieveNotification
    .authenticate()
    .serverLogic { user => notificationId =>
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

  private val getAllNotificationsForCourt = NotificationEndpoints.retrieveNotificationsForCourt
    .authenticate()
    .serverLogic { user => courtId =>
      println(s"Received request to get notifications for court with id: $courtId")

      notificationService
        .retrieveCourtById(courtId)
        .flatMap {
          case Some(_) =>
            println(s"Retrieved court with id $courtId")
            notificationService
              .getAllNotificationsForCourt(courtId)
              .map(_.asRight)
          case None =>
            println(s"Court with id $courtId was not found")
            IO.pure(Left(ResourceNotFound(s"Court with id $courtId not found")))

        }

    }

  private val createNotification = NotificationEndpoints.createNotification
    .authenticate()
    .serverLogic { user => notificationForm =>
      notificationService
        .createNotification(notificationForm)
        .map(_.leftMap(_ => ConflictDescription("Notification with this id already exists")))
    }

  private val deleteNotification = NotificationEndpoints.deleteNotificationEndpoint
    .authenticate()
    .serverLogic { user => notificationId =>
      println(s"Received request to delete notification with id ${notificationId}")
      notificationService.getNotificationById(notificationId).flatMap {
        case Some(notification) =>
          if user.role == UserRole.Admin then
            notificationService
              .deleteNotification(notificationId)
              .map(_.leftMap(_ => NotificationDeletionError("Notification could not be deleted")))
          else

            IO.pure(Left(NotificationDeletionError("User is not authorized to delete reservations")))

        case None => IO.pure(Left(NotificationDeletionError("No such notification")))
      }
    }

  val endpoints: List[ServerEndpoint[Any, IO]] = List(
    getNotification,
    getAllNotificationsForCourt,
    createNotification,
    deleteNotification
  )
