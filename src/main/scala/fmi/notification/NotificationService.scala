package fmi.notification

import cats.data.EitherT
import cats.effect.IO
import cats.syntax.all.*
import fmi.ResourceNotFound
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.court.{Court, CourtId}
import fmi.reservation.ReservationStatus.Placed
import fmi.user.UserRole.{Admin, Owner, Player}
import fmi.user.{User, UserId}
import fmi.utils.DerivationConfiguration.given
import io.circe.Codec
import io.circe.derivation.ConfiguredCodec
import sttp.tapir.Schema

import java.time.Instant
import java.time.temporal.ChronoUnit

class NotificationService(dbTransactor: DbTransactor)(notificationDao: NotificationDao):

  def getNotificationById(notificationId: NotificationId): IO[Option[Notification]] =
    notificationDao.getNotificationById(notificationId)

  def createNotification(notificationForm: NotificationForm): IO[Either[NotificationAlreadyExists, Notification]] = for {
    notificationId <- NotificationId.generate
    createdAt <- IO.realTimeInstant

    notification = Notification(
      notificationId,
      notificationForm.notificationType,
      notificationForm.triggeredBy,
      notificationForm.clubId,
      notificationForm.courtId,
      notificationForm.reservationId,
      NotificationStatus.NotRead,
      createdAt,
      notificationForm.targetUser
    )
    maybeNotification <- notificationDao.createNotification(notification)
  } yield maybeNotification   


  def getAllNotificationsForCourt(courtId: CourtId): IO[List[Notification]] =
    notificationDao.getNotificationsByCourt(courtId)

  def retrieveCourtById(courtId: CourtId): IO[Option[Court]] = notificationDao.retrieveCourtById(courtId)

  def deleteNotification(notificationId: NotificationId): IO[Either[ResourceNotFound, Unit]] =
    println(s"Received request to delete notification with id ${notificationId}")
    notificationDao.deleteReservation(notificationId).flatMap {
      case Right(_) => IO.pure(Right(()))
      case Left(_) => IO.pure(Left(ResourceNotFound("No such notification was found")))
    }

//  def updateReservationStatus(reservationStatusChangeForm: ReservationStatusChangeForm)
//  : IO[Either[ReservationNotFound, Reservation]] =
//    reservationDao
//      .updateReservationStatus(reservationStatusChangeForm.reservationId, reservationStatusChangeForm.reservationStatus)
//      .flatMap {
//        case Right(res) => IO.pure(Right(res))
//        case Left(_) => IO.pure(Left(ReservationNotFound(reservationStatusChangeForm.reservationId)))
//      }

sealed trait NotificationError derives ConfiguredCodec, Schema
case class NotificationNotFound(notificationId: NotificationId) extends NotificationError
case class NotificationAlreadyExists(notificationId: NotificationId) extends NotificationError
