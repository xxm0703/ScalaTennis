package fmi.notification

import cats.effect.IO
import cats.syntax.all.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.postgres.sqlstate
import fmi.court.{CourtId, Court}
import fmi.infrastructure.db.DoobieDatabase.DbTransactor
import fmi.user.UserId

import java.time.Instant

class NotificationDao(dbTransactor: DbTransactor):

  def createNotification(notification: Notification): IO[Either[NotificationAlreadyExists, Notification]] =
    sql"""
        INSERT INTO notification (id, notification_type, triggered_by, club_id, court_id, reservation_id, status, created_at, target_user)
        VALUES (${notification.id},${notification.notificationType}, ${notification.triggeredBy}, ${notification.clubId}, ${notification.courtId}, ${notification.reservationId}, ${notification.status}, ${notification.createdAt}, ${notification.targetUser})
      """.update.run
      .as(notification)
      .attemptSomeSqlState { case sqlstate.class23.UNIQUE_VIOLATION =>
        NotificationAlreadyExists(notification.id)
      }
      .transact(dbTransactor)

  def getNotificationById(notificationId: NotificationId): IO[Option[Notification]] =
    sql"""
      SELECT *
      FROM notification
      WHERE id = $notificationId
    """
      .query[Notification]
      .option
      .transact(dbTransactor)

  def getNotificationsByUser(userId: UserId): IO[List[Notification]] =
    sql"""
      SELECT *
      FROM notification
      WHERE triggered_by = $userId
    """
      .query[Notification]
      .to[List]
      .transact(dbTransactor)

  def getNotificationsByCourt(courtId: CourtId): IO[List[Notification]] =
    sql"""
      SELECT *
      FROM notification
      WHERE court_id = $courtId
    """
      .query[Notification]
      .to[List]
      .transact(dbTransactor)

  def getNotificationsTriggeredByUser(userId: UserId): IO[List[Notification]] =
    sql"""
      SELECT *
      FROM notification
      WHERE triggered_by = $userId
    """
      .query[Notification]
      .to[List]
      .transact(dbTransactor)

  def getNotificationsForUser(userId: UserId): IO[List[Notification]] =
    sql"""
      SELECT *
      FROM notification
      WHERE target_user = $userId AND status = 'not_read'
    """
      .query[Notification]
      .to[List]
      .transact(dbTransactor)

  def getAllNotifications: IO[List[Notification]] =
    sql"""
       SELECT *
       FROM notification
     """
      .query[Notification]
      .to[List]
      .transact(dbTransactor)

  def retrieveCourtById(courtId: CourtId): IO[Option[Court]] =
    sql"""
      SELECT *
      FROM court
      WHERE id = $courtId
    """
      .query[Court]
      .option
      .transact(dbTransactor)

  def deleteReservation(id: NotificationId): IO[Either[NotificationNotFound, Unit]] =
    sql"""
         DELETE FROM notification
         WHERE id = $id
       """.update.run
      .transact(dbTransactor)
      .flatMap {
        case 0 => IO.pure(Left(NotificationNotFound(id)))
        case _ => IO.pure(Right(()))
      }

  def updateNotificationStatus(id: NotificationId, newStatus: NotificationStatus)
    : IO[Either[NotificationNotFound, Notification]] =
    val updateQuery =
      sql"""
          UPDATE notification
          SET status = $newStatus
          WHERE id = $id
        """.update.run

    val retrieveQuery =
      sql"""
          SELECT *
          FROM notification
          WHERE id = $id
        """
        .query[Notification]
        .option

    updateQuery.transact(dbTransactor).flatMap {
      case 0 => IO.pure(Left(NotificationNotFound(id)))
      case _ =>
        retrieveQuery.transact(dbTransactor).map {
          case Some(updatedNotification) => Right(updatedNotification)
          case None =>
            Left(
              NotificationNotFound(id)
            ) // This case is unlikely but handles the case where the reservation is not found after update.
        }
    }
